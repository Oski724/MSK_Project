package MSK_Project.pojazd;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

/**
 * Created by Michal on 2016-04-27.
 */
public class PojazdFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private PojazdAmbassador fedamb;
    private final double timeStep           = 10.0;
    private String typPaliwa;
    private int czasTankowania;
    private boolean czyBrudne;

    public void runFederate() throws RTIexception {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try
        {
            File fom = new File( "stacja-benzynowa.fed" );
            rtiamb.createFederationExecution( "StacjaFederation",
                    fom.toURI().toURL() );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        fedamb = new PojazdAmbassador();
        rtiamb.joinFederationExecution( "PojazdFederate", "StacjaFederation", fedamb );
        log( "Joined Federation as PojazdFederate");

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();

        while (fedamb.running) {
            advanceTime(randomTime());
            sendInteraction(fedamb.federateTime + fedamb.federateLookahead);
            rtiamb.tick();
        }

    }

    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void sendInteraction(double timeStep) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        Random random = new Random();
        int quantityInt = random.nextInt(10) + 1;
        byte[] quantity = EncodingHelpers.encodeInt(quantityInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.GetProduct");
        int quantityHandle = rtiamb.getParameterHandle( "quantity", interactionHandle );

        parameters.add(quantityHandle, quantity);

        LogicalTime time = convertTime( timeStep );
        log("Sending GetProduct: " + quantityInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }

    private void publishAndSubscribe() throws RTIexception {
        //int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.GetProduct" );
        //rtiamb.publishInteractionClass(addProductHandle);
    	
    	////////Publikacja Obiektu////////
    	int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Pojazd"); //Powo³anie obiektu ObjectClassHandle, bêd¹cy wskaznikiem do obiektu
    	AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo³anie obiektu AttributeHandleSet, bêd¹ca list¹ wskazników atrybutów
    	 
        int typPaliwaHandle    = rtiamb.getAttributeHandle( "typPaliwa", classHandle ); //Powo³anie obiektu AttributeHandle, bêd¹cy wskaznikiem do atrybutu
        attributes.add( typPaliwaHandle ); //Dodanie wskaznika do listy wskazników atrybutów

        int idPojazduHandle    = rtiamb.getAttributeHandle( "idPojazdu", classHandle );
        attributes.add( idPojazduHandle );
        
        int czasTankowaniaHandle    = rtiamb.getAttributeHandle( "czasTankowania", classHandle );
        attributes.add( czasTankowaniaHandle );

        int czyBrudnyHandle    = rtiamb.getAttributeHandle( "czyBrudny", classHandle );
        attributes.add( czyBrudnyHandle );
         
        rtiamb.publishObjectClass(classHandle, attributes); //Publikacja Obiektu
        
        /////////Subskrybcja Obiektu Dystrybutor///////////
        int dystrybutorClassHandle = rtiamb.getObjectClassHandle("ObjectRoot.Dystrybutor"); //Powo³anie obiektu ObjectClassHandle, bêd¹cy wskaznikiem do obiektu
    	AttributeHandleSet dytrybutorAttributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo³anie obiektu AttributeHandleSet, bêd¹ca list¹ wskazników atrybutów
    	 
        int typPaliwaDHandle  = rtiamb.getAttributeHandle( "typPaliwa", dystrybutorClassHandle ); //Powo³anie obiektu AttributeHandle, bêd¹cy wskaznikiem do atrybutu
        dytrybutorAttributes.add( typPaliwaDHandle ); //Dodanie wskaznika do listy wskazników atrybutów
        
        int czyWolnyHandle    = rtiamb.getAttributeHandle( "czyWolny", dystrybutorClassHandle );
        dytrybutorAttributes.add( czasTankowaniaHandle );
         
        rtiamb.subscribeObjectClassAttributes(dystrybutorClassHandle, dytrybutorAttributes);
        
        /////////Subskrybcja Obiektu Myjnia///////////
        int myjniaClassHandle = rtiamb.getObjectClassHandle("ObjectRoot.Dystrybutor"); //Powo³anie obiektu ObjectClassHandle, bêd¹cy wskaznikiem do obiektu
    	AttributeHandleSet myjniaAttributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo³anie obiektu AttributeHandleSet, bêd¹ca list¹ wskazników atrybutów
        
        int czyWolnyMHandle    = rtiamb.getAttributeHandle( "czyWolny", myjniaClassHandle );
        myjniaAttributes.add( czyWolnyMHandle );
         
        rtiamb.subscribeObjectClassAttributes(myjniaClassHandle, myjniaAttributes); //Subskrybcja Obiektu
        
        /////////Publikacja Interakcji///////////////
    	int staniecieWKolejceHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.StaniecieWKolejce" ); //Powo³anie obiektu InteractionClassHandle, bêd¹cy wskaznikiem do interakcji
    	rtiamb.publishInteractionClass(staniecieWKolejceHandle); //Publikacja interakcji
    	int tankowanieHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Tankowanie" );
    	rtiamb.publishInteractionClass(tankowanieHandle);
    	int mycieHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.Mycie" );
    	rtiamb.publishInteractionClass(mycieHandle);
    	
    	//////////Subskrybcja Interakcji/////////////
    	int umieszczanieWKolejceHandle = rtiamb.getInteractionClassHandle("InteractionRoot.UmieszczanieWKolejce"); //Powo³anie obiektu InteractionClassHandle, bêd¹cy wskaznikiem do interakcji
    	rtiamb.subscribeInteractionClass(umieszczanieWKolejceHandle); //Subskrybcja na interakcjê
    	int udostepnienieUslugiHandle = rtiamb.getInteractionClassHandle("InteractionRoot.UdostepnienieUslugi");
    	rtiamb.subscribeInteractionClass(udostepnienieUslugiHandle);
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        log("requesting time advance for: " + timestep);
        // request the advance
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private double randomTime() {
        Random r = new Random();
        return 1 +(4 * r.nextDouble());
    }

    private LogicalTime convertTime( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    /**
     * Same as for {@link #convertTime(double)}
     */
    private LogicalTimeInterval convertInterval( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTimeInterval( time );
    }

    private void log( String message )
    {
        System.out.println( "PojazdFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new PojazdFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }


}
