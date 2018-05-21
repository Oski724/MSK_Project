package MSK_Projekt.myjnia;

import java.util.ArrayList;
import java.util.Random;

public class Myjnia {
	
	private int idUslugi;
	private int obslugiwanyPojazd;
	private int czasMycia;
 
    private Boolean czyWolny;
    
    public ArrayList<Integer> kolejka;
    
    public Myjnia(int id, int cM)
    {
    	Random kostka = new Random();
    	this.idUslugi = id;	
    	this.czyWolny = true;
    	this.czasMycia = cM;
    	
    	this.kolejka = new ArrayList<Integer>();
    }
    
    public int getIdUslugi()
    {
    	return idUslugi;
    }
    
    public Boolean getCzyWolny()
    {
    	return czyWolny;
    }
    
    public void setCzyWolny(Boolean cW)
    {
    	this.czyWolny = cW;
    }
    
    public int getObslugiwanyPojazd()
    {
    	return obslugiwanyPojazd;
    }
    
    public void setObslugiwanyPojazd(int oP)
    {
    	this.obslugiwanyPojazd = oP;
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
