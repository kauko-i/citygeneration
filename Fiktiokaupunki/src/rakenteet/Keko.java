package rakenteet;

import generaattori.Kartta.Ruutu;
import java.util.*;

/**
 * Binäärikeko.
 * @author Ilari Kauko
 * @param <T> millaisia olioita keko sisältää
 */
public class Keko<T> {
	
    private static final int ALOITUSKOKO = 256;
	
    /**
     * @author Ilari Kauko
     * @param <T> keon alkioiden tyyppi
     */
	public static interface FT2Double<T> {
		public double f(T t);
	}
	
	
	/**
	 * @author Ilari Kauko
	 * @param <T> keon alkioiden tyyppi
	 */
	public static interface FT2Int<T> {
		public int f(T t);
	}
	
    private FT2Double<T> prioriteetti;
    private FT2Int<T> identifiointi;
    private T[] data;
    private int[] paikat;
    private int koko;

    /**
     * @param p prioriteetti alkioiden lajitteluperuste
     */
	public Keko(FT2Double<T> p) {
        this(p, null, ALOITUSKOKO);
    }
        
	/**
	 * @param p prioriteetti
	 * @param data taulukkomuotoisena annettu valmis data
	 */
    public Keko(FT2Double<T> p, T[] data) {
    	this(p, Arrays.asList(data));
    }
    
	/**
	 * @param p prioriteetti
	 * @param data iteroitavassa muodossa annettu valmis data
	 */
    public Keko(FT2Double<T> p, Iterable<T> data) {
    	this(p, data, null, ALOITUSKOKO);
    }
    
    
    /**
     * @param p prioriteetti
     * @param i alkiot ei-negatiivisilla kokonaisluvuilla yksilöivä funktio
     * @param n edellisen funktion maalijoukon yläraja + 1
     */
    @SuppressWarnings("unchecked")
    public Keko(FT2Double<T> p, FT2Int<T> i, int n) {
        this.prioriteetti = p;
        this.data = (T[])(new Object[n]);
        this.koko = 0;
        if (i == null) return;
        this.identifiointi = i;
        this.paikat = new int[n];
    }
    
    
    /**
     * @param p prioriteetti
     * @param data taulukkomuotoisena annettu valmis data
     * @param i alkiot ei-negatiivisilla kokonaisluvuilla yksilöivä funktio
     * @param n edellisen funktion maalijoukon yläraja + 1
     */
    public Keko(FT2Double<T> p, T[] data, FT2Int<T> i, int n) {
    	this(p, Arrays.asList(data), i, n);
    }
    
    
    /**
     * @param p prioriteetti
     * @param data iteroitavassa muodossa annettu valmis data
     * @param i alkiot ei-negatiivisilla kokonaisluvuilla yksilöivä funktio
     * @param n edellisen funktion maalijoukon yläraja + 1
     */
    public Keko(FT2Double<T> p, Iterable<T> data, FT2Int<T> id, int n) {
    	this(p, id, n);
    	for (T alkio : data) {
    		if (++koko == this.data.length) tuplaaTila();
    		this.data[koko] = alkio;
    		if (paikat != null) paikat[identifiointi.f(alkio)] = koko;
    	}
    	korjaa();
    }
    
    
    /**
     * Lisää kekoon uuden alkion
     * @param alkio kekoon lisättävä alkio
     */
    public void lisaa(T alkio) {
        if (++koko == data.length) tuplaaTila();
        data[koko] = alkio;
        nosta(koko);
    }
    
    
    /**
     * Kaksinkertaistaa keon käyttämän alkiotaulukon koon. Käytetään, jos vanha taulukko täyttyy.
     */
    @SuppressWarnings("unchecked")
    public void tuplaaTila() {
		T[] uusiTaulukko = (T[])(new Object[data.length*2]);
        for (int i = 1; i < data.length; i++) uusiTaulukko[i] = data[i];
        data = uusiTaulukko;
    }
    
    
    /**
     * Hakee, poistaa ja palauttaa keosta prioriteetiltaan pienimmän alkion.
     * @return prioriteetiltaan pienin alkio
     */
    public T pienin() {
        if (koko == 0) return null;
        T palaute = data[1];
        data[1] = data[koko--];
        laske(1);
        return palaute;
    }
    
    
    /**
     * Jos keon epäillään rikkovan kekorakennetta, koko rakenteen korjaus tapahtuu nopeimmin näin.
     */
    public void korjaa() {
    	for (int i = koko/2; i > 0; i--) laske(i);
    }

    
    /**
     * Laskee alkion, jonka epäillään olevan liian ylhäällä, paikkaa.
     * @param i paikka, jossa alkio on
     */
    public void laske(int i) {
        int indeksi = i;
        int lapsi = i;
        if (koko < lapsi) return;
        T siirrettava = data[indeksi];
        double arvo = prioriteetti.f(siirrettava);
        while ((lapsi *= 2) <= koko) {
        	double lapsiarvo = prioriteetti.f(data[lapsi]);
            if (lapsi < koko) {
            	double lapsiarvo2 = prioriteetti.f(data[lapsi+1]);
            	if (lapsiarvo2 < lapsiarvo) {
                	lapsiarvo = lapsiarvo2;
                	lapsi++;
            	}
            }
            if (arvo <= lapsiarvo) break;
            data[indeksi] = data[lapsi];
            if (paikat != null) paikat[identifiointi.f(data[indeksi])] = indeksi;
            indeksi = lapsi;
        }
        data[indeksi] = siirrettava;
        if (paikat != null) paikat[identifiointi.f(siirrettava)] = indeksi;
    }
    
    
    /**
     * Nostaa alkion, jonka epäillään olevan liian alhaalla, paikkaa.
     * @param i paikka, jossa alkio on
     */
    public void nosta(int i) {
        int indeksi;
        int vanhempi = i;
        T nostettava = data[vanhempi];
        double arvo = prioriteetti.f(nostettava);
        while (1 < (indeksi = vanhempi) && arvo < prioriteetti.f(data[vanhempi /= 2])) {
            data[indeksi] = data[vanhempi];
            if (paikat != null) paikat[identifiointi.f(data[indeksi])] = indeksi;
        }
        data[indeksi] = nostettava;
        if (paikat != null) paikat[identifiointi.f(nostettava)] = indeksi;
    }
    
    
    /**
     * Nostaa alkion, jonka epäillään olevan liian alhaalla, paikkaa. Alkion paikka keossa selvitetään identifiointifunktiolla.
     * @param nostettava
     */
    public void nosta(T nostettava) {
    	nosta(paikat[identifiointi.f(nostettava)]);
    }
    
    
    /**
     * Laskee alkion, jonka epäillään olevan liian ylhäällä, paikkaa. Alkion paikka keossa selvitetään identifiointifunktiolla.
     * @param nostettava
     */
    public void laske(T nostettava) {
    	laske(paikat[identifiointi.f(nostettava)]);
    }
    
    
   /**
    * @return keon koko
    */
    public int size() {
        return koko;
    }
    
    
    /**
     * pieni testiohjelma tälle kekorakenteelle
     * @param args ei käytössä
     */
    public static void main(String[] args) {
        
        Ruutu a = new Ruutu(0, 0);
        Ruutu b = new Ruutu(1, 0);
        Ruutu c = new Ruutu(2, 0);
        Ruutu d = new Ruutu(3, 0);
        Ruutu e = new Ruutu(4, 0);

        Ruutu[] ruudut = new Ruutu[] {a, b, c, d, e};
        Keko<Ruutu> keko = new Keko<Ruutu>(r -> Arrays.asList(ruudut).indexOf(r));
        keko.lisaa(a);
        keko.lisaa(e);
        keko.lisaa(c);
        keko.lisaa(b);

        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        keko.lisaa(d);
        keko.lisaa(e);
        keko.lisaa(b);
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
        System.out.println(Arrays.asList(ruudut).indexOf(keko.pienin()));
    }
}