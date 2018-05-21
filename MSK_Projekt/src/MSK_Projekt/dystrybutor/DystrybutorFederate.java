package MSK_Projekt.dystrybutor;


import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import MSK_Project.pojazd.Pojazd;

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
    //private String typPaliwa;
    //private boolean czyWolny;
    //private LinkedList<Integer> kolejka; 

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
        
        /////////Pocz¹tek dzia³ania programu
        
        przygotujDystrybutor(1,"Benzyna");
        przygotujDystrybutor(2,"Olej Napedowy");

        while (fedamb.running) {
            advanceTime(1);
            umozliwienieUslugi();
            //rtiamb.tick();
            //waitForUser();
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

    private void publishAndSubscribe() throws RTIexception {
        //int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddProduct" );
       // rtiamb.publishInteractionClass(addProductHandle);
    	
    	//int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.GetProduct" );
        //rtiamb.publishInteractionClass(addProductHandle);
    	
    	////////Publikacja Obiektu////////
    	int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Dystrybutor"); //Powo³anie obiektu ObjectClassHandle, bêd¹cy wskaznikiem do obiektu
    	AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo³anie obiektu AttributeHandleSet, bêd¹ca list¹ wskazników atrybutów
    	 
        int typPaliwaHandle    = rtiamb.getAttributeHandle( "typPaliwa", classHandle ); //Powo³anie obiektu AttributeHandle, bêd¹cy wskaznikiem do atrybutu
        attributes.add( typPaliwaHandle ); //Dodanie wskaznika do listy wskazników atrybutów
        
        int czyWolnyHandle    = rtiamb.getAttributeHandle( "czyWolny", classHandle );
        attributes.add( czyWolnyHandle );

        int kolejkaHandle    = rtiamb.getAttributeHandle( "kolejka", classHandle );
        attributes.add( kolejkaHandle );
         
        rtiamb.publishObjectClass(classHandle, attributes); //Publikacja Obiektu
        
        /////////Subskrybcja Obiektu Pojazd///////////
        int pojazdClassHandle = rtiamb.getObjectClassHandle("ObjectRoot.Pojazd"); //Powo³anie obiektu ObjectClassHandle, bêd¹cy wskaznikiem do obiektu
    	AttributeHandleSet pojazdAttributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet(); //Powo³anie obiektu AttributeHandleSet, bêd¹ca list¹ wskazników atrybutów
    	 
        int typPaliwaPHandle  = rtiamb.getAttributeHandle( "typPaliwa", pojazdClassHandle ); //Powo³anie obiektu AttributeHandle, bêd¹cy wskaznikiem do atrybutu
        pojazdAttributes.add( typPaliwaPHandle ); //Dodanie wskaznika do listy wskazników atrybutów
        
        int czasTankowaniaHandle    = rtiamb.getAttributeHandle( "czasTankowania", pojazdClassHandle );
        pojazdAttributes.add( czasTankowaniaHandle );
        
        int idPojazduHandle    = rtiamb.getAttributeHandle( "idPojazdu", pojazdClassHandle );
        pojazdAttributes.add( idPojazduHandle );
         
        rtiamb.subscribeObjectClassAttributes(pojazdClassHandle, pojazdAttributes);
 
        
        /////////Publikacja Interakcji///////////////
    	int umieszczanieWKolejceHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.UmieszczanieWKolejce" ); //Powo³anie obiektu InteractionClassHandle, bêd¹cy wskaznikiem do interakcji
    	rtiamb.publishInteractionClass(umieszczanieWKolejceHandle); //Publikacja interakcji
    	int umozliwienieUslugiHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.UmozliwienieUslugi" );
    	rtiamb.publishInteractionClass(umozliwienieUslugiHandle);
    	
    	//////////Subskrybcja Interakcji/////////////
    	int staniecieWKolejceHandle = rtiamb.getInteractionClassHandle("InteractionRoot.StaniecieWKolejce"); //Powo³anie obiektu InteractionClassHandle, bêd¹cy wskaznikiem do interakcji
    	fedamb.staniecieWKolejceHandle = staniecieWKolejceHandle;
    	rtiamb.subscribeInteractionClass(staniecieWKolejceHandle); //Subskrybcja na interakcjê
    	int tankowanieHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Tankowanie");
    	fedamb.tankowanieHandle = tankowanieHandle;
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

    ///////////////Dodane Funkcje////////////
    public void przygotujDystrybutor(int liczba, String typPaliwa)
    {
    	Dystrybutor dystrybutor = new Dystrybutor(liczba,typPaliwa);
    	fedamb.dystrybutory.add(dystrybutor);
    }
    
    private void umozliwienieUslugi() throws RTIexception
    {
    	for(Dystrybutor dystrybutor : fedamb.dystrybutory) {
    		if(dystrybutor.kolejka.size() > 0) {
    			if(dystrybutor.getCzyWolny())
    			{
    				dystrybutor.setObslugiwanyPojazd(dystrybutor.kolejka.get(0));
    				dystrybutor.kolejka.remove(0);
    				dystrybutor.setCzyWolny(false);
    				log("umozliwienieUslugi: Pojazd "+ dystrybutor.getObslugiwanyPojazd() + " ma umozliwiona usluge");
    				wyslijUmozliwienieUslugi(dystrybutor.getObslugiwanyPojazd());
    			}
    		}
    	}
    }
    
    private void wyslijUmieszczanieWKolejce(int idU) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        int idUslugiInt = idU;
        byte[] idUslugi = EncodingHelpers.encodeInt(idUslugiInt);
        
        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.UmieszczanieWKolejce");
        int idUslugiHandle = rtiamb.getParameterHandle( "idUslugi", interactionHandle );
        
        parameters.add(idUslugiHandle, idUslugi);

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        log("Wyslanie Interakcji UmieszczanieWKolejce: " + idUslugiInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }
    
    private void wyslijUmozliwienieUslugi(int idU) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        int idUslugiInt = idU;
        byte[] idUslugi = EncodingHelpers.encodeInt(idUslugiInt);
        
        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.UmozliwienieUslugi");
        int idUslugiHandle = rtiamb.getParameterHandle( "idUslugi", interactionHandle );
        
        parameters.add(idUslugiHandle, idUslugi);

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        log("Wyslanie Interakcji UmozliwienieUslugi: " + idUslugiInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }
    
}
