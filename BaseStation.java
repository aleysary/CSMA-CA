package csma;
import java.awt.Color;

public class BaseStation extends Terminal
{
    private MobileStation curSender;
    
    class State
    {
	static final int idle = 0;
	static final int SIFS_before_emitCTS = 1;
	static final int emitCTS = 2;
	static final int SIFS_before_rcvPKT = 3;
	static final int rcvPKT = 4;
	static final int SIFS_before_emitACK = 5;
	static final int emitACK = 6;
    }
    
    public BaseStation(String string, SimTimer simtimer) {
	super(string, simtimer);
    }
    

    //Implement Protocol State Machine 
    	private void activeAction() {
    	    switch (state) {
    	        case State.idle:
    	            // Do nothing, waiting for RTS packets
    	            break;

    	        case State.SIFS_before_emitCTS:
    	            if (elapsedTime(3)) { // Delay of 3 units
    	                changeState(State.emitCTS);
    	            }
    	            break;

    	        case State.emitCTS:
    	            if (elapsedTime(15)) { // Delay of 15 units
    	                myChannel.receptionAction(emmitedPacket); // Send CTS packet
    	                changeState(State.SIFS_before_rcvPKT);
    	            }
    	            break;

    	        case State.SIFS_before_rcvPKT:
    	            if (elapsedTime(3)) { // Delay of 3 units
    	                changeState(State.rcvPKT);
    	            }
    	            break;

    	        case State.rcvPKT:
    	            if (elapsedTime(61)) { // Timeout
    	                changeState(State.idle); // Return to idle if no packet received
    	                curSender = null; // Clear current sender
    	            }
    	            // Correct packet handling is managed in receptionAction
    	            break;

    	        case State.SIFS_before_emitACK:
    	            if (elapsedTime(3)) { // Delay of 3 units
    	                changeState(State.emitACK);
    	            }
    	            break;

    	        case State.emitACK:
    	            if (elapsedTime(10)) { // Delay of 10 units
    	                myChannel.receptionAction(emmitedPacket); // Send ACK packet
    	                changeState(State.idle); // Return to idle
    	                curSender = null; // Clear current sender
    	            }
    	            break;
    	    }
    	


    }
    
    protected void changeState(int i) {
	super.changeState(i);
	if (i == 2)
	    emmitedPacket = new Packet(curSender, 2);
	if (i == 6)
	    emmitedPacket = new Packet(curSender, 4);
    }
    
    public boolean emmiting() {
	return state == 6 || state == 2;
    }
    
    protected Color getChannelColor() {
	return Color.white;
    }
    
    protected Color getStateColor() {
	Color color = Color.white;
	switch (state) {
	case 2:
	    color = new Color(0, 0, 128);
	    break;
	case 6:
	    color = new Color(128, 0, 0);
	    break;
	}
	return color;
    }

    // Implement Protocol State Machine (Receive portion)
    	public void receptionAction(Packet packet) {
    	    if (state == State.idle) {
    	        // Handle RTS packet
    	        if (!packet.getCorrupted(this) && packet.getType() == Packet.RTS) {
    	            curSender = (MobileStation) packet.getOwner(); // Set current sender
    	            changeState(State.SIFS_before_emitCTS); // Prepare to send CTS
    	        }
    	    } else if (state == State.rcvPKT) {
    	        // Handle PKT packet
    	        if (!packet.getCorrupted(this) && packet.getType() == Packet.PKT && packet.getOwner() == curSender) {
    	            changeState(State.SIFS_before_emitACK); // Prepare to send ACK
    	        } else {
    	            changeState(State.idle); // Return to idle if conditions fail
    	            curSender = null; // Clear current sender
    	        }
    	    }
    	}

 
    public void update() {
	activeAction();
	this.graphicUpdate();
    }
}
