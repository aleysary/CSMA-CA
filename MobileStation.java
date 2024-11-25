package csma;
import java.awt.Color;
import java.awt.Label;
import java.util.Random;

public class MobileStation extends Terminal
{
    private BaseStation BS;
    private int packetToSend;
    private int CWorder;
    private int backoffValue;
    private int nav;
    private static Random rand = new Random();
    private Label queueLBL;
    private Label backoffLBL;
    
    public class State
    {
	static final int idle = 0;
	static final int DIFS_beforeCountdown = 1;
	static final int countdown = 2;
	static final int emitRTS = 3;
	static final int SIFS_before_rcvCTS = 4;
	static final int rcvCTS = 5;
	static final int SIFS_before_emitPKT = 6;
	static final int emitPKT = 7;
	static final int SIFS_before_rcvACK = 8;
	static final int rcvACK = 9;
    }
    
    public MobileStation(String string, SimTimer simtimer,
			 BaseStation basestation) {
	super(string, simtimer);
	BS = basestation;
	changeState(0);
	packetToSend = 0;
    }
    
    // Implement Backoff Function (Random Exponential)
    private void BEB() {
        CWorder = Math.min(CWorder + 1, 10); // Increment CWorder, capped at 10
        int maxBackoff = (int) Math.pow(2, CWorder) - 1;
        backoffValue = rand.nextInt(maxBackoff + 1); // Generate random backoff value
    }
    
    // Implement the protocol as a state machine
    private void activeAction() {
        switch (state) {
            case State.idle:
                if (packetToSend > 0) { // Check if there are packets to send
                    if (myChannel.getBusy() || nav > 0) {
                        BEB(); // Perform backoff if channel is busy or NAV is active
                    }
                    changeState(State.DIFS_beforeCountdown);
                }
                break;

            case State.DIFS_beforeCountdown:
                if (elapsedTime(5)) { // DIFS delay
                    changeState(State.countdown);
                } else if (myChannel.getBusy() || nav > 0) {
                    changeState(State.DIFS_beforeCountdown); // Wait if channel is busy or NAV is active
                }
                break;

            case State.countdown:
                if (elapsedTime(5)) { // Countdown delay
                    backoffValue--;
                    if (backoffValue <= 0) {
                        changeState(State.emitRTS); // Transition to emitRTS if backoff is complete
                    }
                } else if (myChannel.getBusy() || nav > 0) {
                    changeState(State.idle); // Return to idle if channel is busy or NAV is active
                }
                break;

            case State.emitRTS:
                if (elapsedTime(15)) { // RTS transmission delay
                    myChannel.receptionAction(emmitedPacket); // Send RTS packet
                    changeState(State.SIFS_before_rcvCTS);
                }
                break;

            case State.SIFS_before_rcvCTS:
                if (elapsedTime(3)) { // SIFS delay
                    changeState(State.rcvCTS);
                }
                break;

            case State.rcvCTS:
                if (elapsedTime(16)) { // CTS reception delay
                    BEB(); // Backoff if timeout occurs
                    changeState(State.DIFS_beforeCountdown);
                }
                break;

            case State.SIFS_before_emitPKT:
                if (elapsedTime(3)) { // SIFS delay
                    changeState(State.emitPKT);
                }
                break;

            case State.emitPKT:
                if (elapsedTime(60)) { // PKT transmission delay
                    myChannel.receptionAction(emmitedPacket); // Send PKT
                    changeState(State.SIFS_before_rcvACK);
                }
                break;

            case State.SIFS_before_rcvACK:
                if (elapsedTime(3)) { // SIFS delay
                    changeState(State.rcvACK);
                }
                break;

            case State.rcvACK:
                if (elapsedTime(11)) { // ACK reception delay
                    BEB(); // Backoff on timeout
                    changeState(State.DIFS_beforeCountdown);
                }
                break;
        }
    }

    
    public void addPktToSendingQueue() {
	packetToSend++;
	txtUpdate();
    }
    
    protected void changeState(int i) {
	super.changeState(i);
	if (i == 2 && backoffValue == 0)
	    changeState(3);
	if (i == 0)
	    CWorder = 3;
	if (i == 3)
	    emmitedPacket = new Packet(this, 1);
	if (i == 7)
	    emmitedPacket = new Packet(this, 3);
    }
    
    public boolean emmiting() {
	return state == 3 || state == 7;
    }
    
    protected Color getChannelColor() {
	if (nav > 0)
	    return Color.orange;
	if (myChannel.getBusy())
	    return Color.red;
	return Color.green;
    }
    
    protected Color getStateColor() {
	Color color = Color.white;
	switch (state) {
	case 3:
	    color = new Color(0, 0, 255);
	    break;
	case 7:
	    color = new Color(175, 0, 0);
	    break;
	case 2:
	    color = Color.lightGray;
	    break;
	}
	return color;
    }
    
    protected void graphicUpdate() {
	super.graphicUpdate();
	txtUpdate();
    }

    // Implement the protocol state machine
    public void receptionAction(Packet packet) {
        if (state == State.idle || state == State.DIFS_beforeCountdown || state == State.countdown) {
            // Update NAV if necessary
            if (!packet.getCorrupted(this) && packet.getNav() > nav) {
                nav = packet.getNav();
            }
        } else if (state == State.rcvCTS) {
            // Handle CTS packet
            if (!packet.getCorrupted(this) && packet.getType() == Packet.CTS && packet.getOwner() == this) {
                changeState(State.SIFS_before_emitPKT); // Prepare to send PKT
            } else {
                BEB(); // Backoff if CTS is invalid
                changeState(State.DIFS_beforeCountdown);
            }
        } else if (state == State.rcvACK) {
            // Handle ACK packet
            if (!packet.getCorrupted(this) && packet.getType() == Packet.ACK && packet.getOwner() == this) {
                packetToSend--; // Decrement the packet queue
                if (packetToSend > 0) {
                    CWorder = 3; // Reset CWorder
                    BEB();
                    changeState(State.DIFS_beforeCountdown);
                } else {
                    changeState(State.idle); // No more packets to send
                }
            } else {
                BEB(); // Backoff if ACK is invalid
                changeState(State.DIFS_beforeCountdown);
            }
        }
    }

    
    public void setDisplay(Label label, Label label_0_, TimeLine timeline) {
	queueLBL = label;
	backoffLBL = label_0_;
	super.setDisplay(timeline);
    }
    
    private void txtUpdate() {
	queueLBL.setText(Integer.toString(packetToSend));
	backoffLBL.setText(Integer.toString(backoffValue));
    }
    
    public void update() {
	nav--;
	if (nav < 0)
	    nav = 0;
	activeAction();
	graphicUpdate();
    }
}
