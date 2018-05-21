package MSK_Project.pojazd;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import MSK_Projekt.myjnia.ExternalEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

public class PojazdFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private PojazdAmbassador fedamb;
    private final double timeStep           = 10.0;
    private int pojazdHlaHandle;

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
        
        registerPojazdObject();
        

        while (fedamb.running) {
            advanceTime(1);
            przygotujPojazdy(1);
            staniecieWKolejce();
            tankowanie();
            mycie();
            //wyslijStaniecieWKolejce(0);
            //rtiamb.tick();
            waitForUser();
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
        dytrybutorAttributes.add( czyWolnyHandle );
         
        int kolejkaHandle    = rtiamb.getAttributeHandle( "kolejka", dystrybutorClassHandle );
        dytrybutorAttributes.add( kolejkaHandle );
        
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
    	fedamb.umieszczanieWKolejceHandle = umieszczanieWKolejceHandle;
    	rtiamb.subscribeInteractionClass(umieszczanieWKolejceHandle); //Subskrybcja na interakcjê
    	int umozliwienieUslugiHandle = rtiamb.getInteractionClassHandle("InteractionRoot.UmozliwienieUslugi");
    	fedamb.umozliwienieUslugiHandle = umozliwienieUslugiHandle;
    	rtiamb.subscribeInteractionClass(umozliwienieUslugiHandle);
    }
    
    private void registerPojazdObject() throws RTIexception {
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Pojazd");
        this.pojazdHlaHandle = rtiamb.registerObjectInstance(classHandle);
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

    ///////////////Dodane Funkcje////////////
    public void przygotujPojazdy(int liczba)
    {
    	Random kostka = new Random();
    	int wyn = kostka.nextInt(10)+1;
    	if(wyn > 8) {
	    	int ostatniId;
	    	int size = fedamb.pojazdy.size();
	    	if(size > 0)
	    	{
	    		ostatniId = fedamb.pojazdy.get(size-1).getId();
	    	}
	    	else
	    	{
	    		ostatniId = 0;
	    	}
	    	for(int i = ostatniId+1; i<ostatniId+liczba+1; ++i)
	    	{
	    		Pojazd pojazd = new Pojazd(i);
	    		fedamb.pojazdy.add(pojazd);
	    		log("Pojazd " + i + " przygotowany. Razem: " + fedamb.pojazdy.size());
	    	}
    	}
    }
    
    
    private void staniecieWKolejce() throws RTIexception
    {
    	for(Pojazd pojazd : fedamb.pojazdy) {
    		if(pojazd.getStanPojazdu() == "Szuka dystrybutora") {
    			log("StaniecieWKolejce: Proba pojazdu " + pojazd.getId());
    			wyslijStaniecieWKolejce(pojazd.getId(),pojazd.getTypPaliwa());
    			pojazd.setStanPojazdu("Dystrybutor Znaleziony");
    		}
    		else if(pojazd.getStanPojazdu() == "Szuka myjni") {
    			log("StaniecieWKolejce: Proba pojazdu " + pojazd.getId());
    			wyslijStaniecieWKolejce(pojazd.getId(),"myjnia");
    			pojazd.setStanPojazdu("Myjnia Znaleziona");
    		}
    	}
    }
    
    private void tankowanie() throws RTIexception
    {
    	for(Pojazd pojazd : fedamb.pojazdy) {
    		if(pojazd.getStanPojazdu() == "Rozpoczecie Uslugi" || pojazd.getStanPojazdu() == "Tankuje") {
    			if(pojazd.getCzasMycia() < 1)
    			{
	    			if(pojazd.getCzasTankowania() > 0)
	    			{
	    				pojazd.setCzasTankowania(pojazd.getCzasTankowania() - 1);
	    				log("Tankowanie: Pojazd " + pojazd.getId() + " tankuje. Pozostalo czasu: " + pojazd.getCzasTankowania());
	    				pojazd.setStanPojazdu("Tankuje");
	    			}
	    			else
	    			{
		    			wyslijTankowanie(pojazd.getId());
		    			log("Tankowanie: Pojazd " + pojazd.getId() + " skonczyl tankowanie.");
		    			if(pojazd.getCzyBrudne())
		    			{
		    				pojazd.setStanPojazdu("Szuka myjni");
		    			}
		    			else
		    			{
		    				pojazd.setStanPojazdu("Szuka kasy");
		    			}
	    			}
	    		}
    		}
    	}
    }
    	
	private void mycie() throws RTIexception
    {
    	for(Pojazd pojazd : fedamb.pojazdy) {
    		if(pojazd.getCzasTankowania() < 1) {
	    		if(pojazd.getStanPojazdu() == "Rozpoczecie Uslugi" || pojazd.getStanPojazdu() == "Myje") {
	    			if(pojazd.getCzasMycia() > 0)
	    			{
	    				pojazd.setCzasMycia(pojazd.getCzasMycia() - 1);
	    				log("Mycie: Pojazd " + pojazd.getId() + " jest myty. Pozostalo czasu: " + pojazd.getCzasMycia());
	    				pojazd.setStanPojazdu("Myje");
	    			}
	    			else
	    			{
		    			wyslijMycie(pojazd.getId());
		    			log("Mycie: Pojazd " + pojazd.getId() + " skonczyl mycie.");
		    			pojazd.setStanPojazdu("Szuka kasy");
	    			}
	    		}
    		}
    	}
    }
    
    private void wyslijStaniecieWKolejce(int idP, String tP) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        int idPojazduInt = idP;
        byte[] idPojazdu = EncodingHelpers.encodeInt(idPojazduInt);
        String typPaliwaString = tP;
        byte[] typPaliwa = EncodingHelpers.encodeString(typPaliwaString);
        

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.StaniecieWKolejce");
        int idPojazduHandle = rtiamb.getParameterHandle( "idPojazdu", interactionHandle );
        int typPaliwaHandle = rtiamb.getParameterHandle( "typPaliwa", interactionHandle );
        
        parameters.add(idPojazduHandle, idPojazdu);
        parameters.add(typPaliwaHandle, typPaliwa);

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        log("Wyslanie Interakcji StaniecieWKolejce: " + idPojazduInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }
    
    private void wyslijTankowanie(int idP) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        int idPojazduInt = idP;
        byte[] idPojazdu = EncodingHelpers.encodeInt(idPojazduInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Tankowanie");
        int idPojazduHandle = rtiamb.getParameterHandle( "idPojazdu", interactionHandle );
        
        parameters.add(idPojazduHandle, idPojazdu);

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        log("Wyslanie Interakcji Tankowanie: " + idPojazduInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }
    
    private void wyslijMycie(int idP) throws RTIexception {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        int idPojazduInt = idP;
        byte[] idPojazdu = EncodingHelpers.encodeInt(idPojazduInt);

        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.Mycie");
        int idPojazduHandle = rtiamb.getParameterHandle( "idPojazdu", interactionHandle );
        
        parameters.add(idPojazduHandle, idPojazdu);

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        log("Wyslanie Interakcji Mycie: " + idPojazduInt);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }
    
    
    
    
    
}
