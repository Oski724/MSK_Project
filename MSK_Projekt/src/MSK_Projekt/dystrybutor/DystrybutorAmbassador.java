package MSK_Projekt.dystrybutor;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import java.util.ArrayList;
import java.util.LinkedList;

//import hla13.Example13Federate;
import org.portico.impl.hla13.types.DoubleTime;

import MSK_Project.pojazd.Pojazd;


public class DystrybutorAmbassador extends NullFederateAmbassador {

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;
    
    protected int staniecieWKolejceHandle = 0;
    protected int tankowanieHandle = 0;

    public ArrayList<Dystrybutor> dystrybutory; 
    
    public DystrybutorAmbassador()
    {
    	this.dystrybutory = new ArrayList<Dystrybutor>();
    }

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "DystrybutorAmbassador: " + message );
    }

    public void synchronizationPointRegistrationFailed( String label )
    {
        log( "Failed to register sync point: " + label );
    }

    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(DystrybutorFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(DystrybutorFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }

    public void receiveInteraction( int interactionClass,
            ReceivedInteraction theInteraction,
            byte[] tag,
            LogicalTime theTime,
            EventRetractionHandle eventRetractionHandle )
	{
		//StringBuilder builder = new StringBuilder( "Interaction Received:" );
		log( "Otrzymana Interakcja:" + interactionClass);
		try {
			int idP = EncodingHelpers.decodeInt(theInteraction.getValue(0));
			
			if(interactionClass == staniecieWKolejceHandle && theInteraction.size() > 1) {
				
				String tP = EncodingHelpers.decodeString(theInteraction.getValue(1));
				odpowiedzStaniecieWKolejce(idP,tP);
				
			} else if (interactionClass == tankowanieHandle) {
				
				odpowiedzTankowanie(idP);
			}
        } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
            arrayIndexOutOfBounds.printStackTrace();
        }
	}
    
    public void odpowiedzStaniecieWKolejce(int idP,String tP)
    {
    	log("StaniecieWKolejce: Proba pojazdu " + idP + ". Typ paliwa: " + tP);
    	//String typPaliwaP = "Benzyna";
    	String typPaliwaP = tP;
    	for(Dystrybutor dystrybutor : this.dystrybutory)
    	{
    		if(typPaliwaP.equals(dystrybutor.getTypPaliwa()))
    		{
    			dystrybutor.kolejka.add(idP);
    			log("StaniecieWKolejce: Dystrybutor " + dystrybutor.getIdUslugi()+". Razem:" + dystrybutor.kolejka.size());
    		}
    	}
    }
    
    public void odpowiedzTankowanie(int idP)
    {
    	for(Dystrybutor dystrybutor : this.dystrybutory)
    	{
    		if(dystrybutor.getObslugiwanyPojazd() == idP)
    		{
    			log("Tankowanie: Pojazd " + dystrybutor.getObslugiwanyPojazd() + " skonczyl tankowanie. Dystrybutor " + dystrybutor.getIdUslugi() + " jest zwolniony.");
    			dystrybutor.setObslugiwanyPojazd(0);
    			dystrybutor.setCzyWolny(true);
    		}
    	}
    }
    
}
