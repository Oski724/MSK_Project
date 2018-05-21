package MSK_Projekt.myjnia;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.Example13Federate;
import org.portico.impl.hla13.types.DoubleTime;

import MSK_Projekt.dystrybutor.Dystrybutor;

import java.util.ArrayList;


public class MyjniaAmbassador extends NullFederateAmbassador {

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;

    protected int staniecieWKolejceHandle = 0;
    protected int mycieHandle = 0;

    public ArrayList<Myjnia> myjnie; 
    
    public MyjniaAmbassador()
    {
    	this.myjnie = new ArrayList<Myjnia>();
    }

    private double convertTime( LogicalTime logicalTime )
    {
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "MyjniaAmbassador: " + message );
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
        if( label.equals(MyjniaFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(MyjniaFederate.READY_TO_RUN) )
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
				String cM = EncodingHelpers.decodeString(theInteraction.getValue(1));
				odpowiedzStaniecieWKolejceHandle(idP,cM);
				
			} else if (interactionClass == mycieHandle) {
				
				odpowiedzMycie(idP);
			}
        } catch (ArrayIndexOutOfBounds arrayIndexOutOfBounds) {
            arrayIndexOutOfBounds.printStackTrace();
        }
	}
    
    public void odpowiedzStaniecieWKolejceHandle(int idP,String cM)
    {
    	//log("StaniecieWKolejce: Proba pojazdu " + idP);
    	for(Myjnia myjnia : this.myjnie)
    	{
    		if(cM.equals("myjnia"))
    		{
    			myjnia.kolejka.add(idP);
    			log("StaniecieWKolejce: Myjnia " + myjnia.getIdUslugi()+". Razem:" + myjnia.kolejka.size());
    		}
    	}
    }
    
    public void odpowiedzMycie(int idP)
    {
    	for(Myjnia myjnia : this.myjnie)
    	{
    		if(myjnia.getObslugiwanyPojazd() == idP)
    		{
    			log("Mycie: Pojazd " + myjnia.getObslugiwanyPojazd() + " skonczyl mycie. Myjnia " + myjnia.getIdUslugi() + " jest zwolniona.");
    			myjnia.setObslugiwanyPojazd(0);
    			myjnia.setCzyWolny(true);
    		}
    	}
    }

    

}
