package MSK_Project.pojazd;

import java.util.Random;

public class Pojazd {
	
	private int id;
	private String typPaliwa;
    private int czasTankowania;
    private boolean czyBrudne;
    
    private int czasMycia;
    
    private String stanPojazdu;
    
    public Pojazd(int id)
    {
    	Random kostka = new Random();
    	this.id = id;
    	this.czasTankowania = kostka.nextInt(5)+1;
    	//this.czasTankowania = 0;
    	boolean tp = kostka.nextBoolean();
    	if(tp)
    	{
    		this.typPaliwa = "Benzyna";
    	}
    	else
    	{
    		this.typPaliwa = "Olej Napedowy";
    	}
    	
    	this.czyBrudne = kostka.nextBoolean();
    	this.stanPojazdu = "Szuka dystrybutora";
    }
    
    public int getId()
    {
    	return id;
    }
    
    public int getCzasTankowania()
    {
    	return czasTankowania;
    }
    
    public void setCzasTankowania(int cT)
    {
    	this.czasTankowania = cT;
    }
    
    public boolean getCzyBrudne()
    {
    	return czyBrudne;
    }
    
    public String getTypPaliwa()
    {
    	return typPaliwa;
    }

    public String getStanPojazdu()
    {
    	return stanPojazdu;
    }
    
    public void setStanPojazdu(String stan)
    {
    	this.stanPojazdu = stan;
    }

    public int getCzasMycia()
    {
    	return czasMycia;
    }
    
    public void setCzasMycia(int cM)
    {
    	this.czasMycia = cM;
    }
}
