package MSK_Projekt.dystrybutor;


import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.Random;

public class DystrybutorFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private DystrybutorAmbassador fedamb;
    private final double timeStep           = 10.0;
    private String typPaliwa;
    private boolean czyWolny;
    private LinkedList<Integer> kolejka; 

    public void runFederate() throws RTIexception{
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

        fedamb = new DystrybutorAmbassador();
        rtiamb.joinFederationExecution( "DystrybutorFederate", "StacjaFederation", fedamb );
        log( "Joined Federation as DystrybutorFederate");

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
        byte[] quantity = EncodingHelpers.encodeInt(random.nextInt(10) + 1);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddProduct");
        int quantityHandle = rtiamb.getParameterHandle( "quantity", interactionHandle );

        parameters.add(quantityHandle, quantity);

        LogicalTime time = convertTime( timeStep );
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }

    private void publishAndSubscribe() throws RTIexception {
        //int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddProduct" );
       // rtiamb.publishInteractionClass(addProductHandle);
    	
    	//int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.GetProduct" );
        //rtiamb.publishInteractionClass(addProductHandle);
    	
    	////////Publikacja Obiektu////////
    	int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Dystrybutor"); //Powo�anie obiektu ObjectClassHandle, b�d�cy wskaznikiem do obiektu
    	AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo�anie obiektu AttributeHandleSet, b�d�ca list� wskaznik�w atrybut�w
    	 
        int typPaliwaHandle    = rtiamb.getAttributeHandle( "typPaliwa", classHandle ); //Powo�anie obiektu AttributeHandle, b�d�cy wskaznikiem do atrybutu
        attributes.add( typPaliwaHandle ); //Dodanie wskaznika do listy wskaznik�w atrybut�w
        
        int czyWolnyHandle    = rtiamb.getAttributeHandle( "czyWolny", classHandle );
        attributes.add( czyWolnyHandle );

        int kolejkaHandle    = rtiamb.getAttributeHandle( "kolejkaHandle", classHandle );
        attributes.add( kolejkaHandle );
         
        rtiamb.publishObjectClass(classHandle, attributes); //Publikacja Obiektu
        
        /////////Subskrybcja Obiektu Pojazd///////////
        int pojazdClassHandle = rtiamb.getObjectClassHandle("ObjectRoot.Pojazd"); //Powo�anie obiektu ObjectClassHandle, b�d�cy wskaznikiem do obiektu
    	AttributeHandleSet pojazdAttributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo�anie obiektu AttributeHandleSet, b�d�ca list� wskaznik�w atrybut�w
    	 
        int typPaliwaPHandle  = rtiamb.getAttributeHandle( "typPaliwa", pojazdClassHandle ); //Powo�anie obiektu AttributeHandle, b�d�cy wskaznikiem do atrybutu
        pojazdAttributes.add( typPaliwaPHandle ); //Dodanie wskaznika do listy wskaznik�w atrybut�w
        
        int czasTankowaniaHandle    = rtiamb.getAttributeHandle( "czasTankowania", pojazdClassHandle );
        pojazdAttributes.add( czasTankowaniaHandle );
        
        int idPojazduHandle    = rtiamb.getAttributeHandle( "idPojazdu", pojazdClassHandle );
        pojazdAttributes.add( idPojazduHandle );
         
        rtiamb.subscribeObjectClassAttributes(pojazdClassHandle, pojazdAttributes);
 
        
        /////////Publikacja Interakcji///////////////
    	int umieszczenieWKolejceHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.UmieszczenieWKolejce" ); //Powo�anie obiektu InteractionClassHandle, b�d�cy wskaznikiem do interakcji
    	rtiamb.publishInteractionClass(umieszczenieWKolejceHandle); //Publikacja interakcji
    	int udostepnienieUslugiHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.UdostepnienieUslugi" );
    	rtiamb.publishInteractionClass(udostepnienieUslugiHandle);
    	
    	//////////Subskrybcja Interakcji/////////////
    	int staniecieWKolejceHandle = rtiamb.getInteractionClassHandle("InteractionRoot.StaniecieWKolejce"); //Powo�anie obiektu InteractionClassHandle, b�d�cy wskaznikiem do interakcji
    	rtiamb.subscribeInteractionClass(staniecieWKolejceHandle); //Subskrybcja na interakcj�
    	int tankowanieHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Tankowanie");
    	rtiamb.subscribeInteractionClass(tankowanieHandle);
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
        return 1 +(9 * r.nextDouble());
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
        System.out.println( "DystrybutorFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new DystrybutorFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
