package MSK_Project.pojazd;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import java.util.ArrayList;

//import hla13.Example13Federate;
import org.portico.impl.hla13.types.DoubleTime;

import MSK_Projekt.dystrybutor.Dystrybutor;
import MSK_Projekt.myjnia.ExternalEvent;

public class PojazdAmbassador extends NullFederateAmbassador {

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;
    protected int umieszczanieWKolejceHandle = 0;
    protected int umozliwienieUslugiHandle = 0;
    
    public ArrayList<Pojazd> pojazdy;
    
    public PojazdAmbassador()
    {
    	this.pojazdy = new ArrayList<Pojazd>();
    }

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "PojazdAmbassador: " + message );
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
        if( label.equals(PojazdFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(PojazdFederate.READY_TO_RUN) )
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
		//StringBuilder builder = new StringBuilder( "Interaction Received:" + interactionClass);
		log( "Otrzymana Interakcja:" + interactionClass);
		try {
			int idP = EncodingHelpers.decodeInt(theInteraction.getValue(0));
		
			if(interactionClass == umieszczanieWKolejceHandle) {
				odpowiedzUmieszczenieWKolejce(idP);
			} else if (interactionClass == umozliwienieUslugiHandle) {
				if(theInteraction.size() > 1)
				{
					int cM = EncodingHelpers.decodeInt(theInteraction.getValue(1));
					odpowiedzUmozliwienieUslugi(idP,cM);
				}
				else
				{
					odpowiedzUmozliwienieUslugi(idP);
				}
			}
		
		} catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
            arrayIndexOutOfBounds.printStackTrace();
        }
		
	}
    
    public void odpowiedzUmieszczenieWKolejce(int idP)
    {
    }
    
    public void odpowiedzUmozliwienieUslugi(int idP)
    {
    	log("UmozliwienieUslugi: Dla pojazdu " + idP);
    	for(Pojazd pojazd : this.pojazdy)
    	{
    		if(pojazd.getId() == idP)
    		{
    			pojazd.setStanPojazdu("Rozpoczecie Uslugi");
    		}
    	}
    }

    public void odpowiedzUmozliwienieUslugi(int idP,int cM)
    {
    	log("UmozliwienieUslugi: Dla pojazdu " + idP);
    	for(Pojazd pojazd : this.pojazdy)
    	{
    		if(pojazd.getId() == idP)
    		{
    			pojazd.setStanPojazdu("Rozpoczecie Uslugi");
    			pojazd.setCzasMycia(cM);
    		}
    	}
    }
}
