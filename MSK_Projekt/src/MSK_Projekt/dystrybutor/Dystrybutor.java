package MSK_Projekt.dystrybutor;

import java.util.ArrayList;
import java.util.Random;

import MSK_Project.pojazd.Pojazd;

public class Dystrybutor {

	private int idUslugi;
	private String typPaliwa;
	private int obslugiwanyPojazd;
 
    private Boolean czyWolny;
    
    public ArrayList<Integer> kolejka;
    
    public Dystrybutor(int id, String tP)
    {
    	Random kostka = new Random();
    	this.idUslugi = id;	
    	this.typPaliwa = tP;
    	this.czyWolny = true;
    	
    	this.kolejka = new ArrayList<Integer>();
    }
    
    public int getIdUslugi()
    {
    	return idUslugi;
    }
    
    public String getTypPaliwa()
    {
    	return typPaliwa;
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
}
