package generaattori;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;
import java.util.Stack;

import javax.imageio.ImageIO;

import matikka.Vektori;
import rakenteet.Keko;
import matikka.Funktiot;


import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * kaupunkikarttageneraattori
 * @author Ilari Kauko
 */
public class Kartta {
	
	// rajapintoja lambdafunktioille
	public static interface FunktioRuutuBoolean {
		public boolean f(Ruutu r);
	}
	
	public static interface FunktioRuutuDouble {
		public double f(Ruutu r);
	}
	
	public static interface FunktioRuutuInt {
		public int f(Ruutu r);
	}
    
	public static interface FunktioRuutuVoid {
		public void f(Ruutu r);
	}
	
	public static interface Funktio2RuutuaDouble {
		public double f(Ruutu a, Ruutu b);
	}
	
	public static interface Funktio2RuutuaDoubleDouble {
		public double f(Ruutu a, Ruutu b, double x);
	}
	
	public static interface FunktioRuutuRuutulist {
		public ArrayList<Ruutu> f(Ruutu r);
	}
	
	public static interface Funktio2RuutuaInt {
		public int f(Ruutu a, Ruutu b);
	}
	
	/**
	 * Kartta käsitellään kaksiuloitteisena ruututaulukkona, jossa ruutu on oma luokkansa.
	 * Idea on, että kukin ruutu vastaa 25 neliömetriä, mutta vain piirra-metodi suorastaan velvoittaa tätä.
	 * @author Ilari Kauko
	 */
    public static class Ruutu {
        
        private int x, y, maankaytto, rakennus, katu, tontti, e;
        private double korkeus;
        private boolean rataa;
        
        /**
         * @param x ruudun x-koordinaatti
         * @param y ruudun y-koordinaatti
         */
        public Ruutu(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        
        /**
         * Muokkaa ruudun tiedot vastaamaan annettua merkkijonokuvausta.
         * @param kuvaus Merkkijono, jossa ruudun tiedot on kuvattu.
         */
        public void parse(String kuvaus) {
            String[] data = kuvaus.split("\\|");
            maankaytto = Integer.parseInt(data[0]);
            katu = Integer.parseInt(data[1]);
            rakennus = Integer.parseInt(data[2]);
            tontti = Integer.parseInt(data[3]);
            e = Integer.parseInt(data[4]);
            korkeus = Double.parseDouble(data[5]);
            rataa = Boolean.parseBoolean(data[6]);
        }
        
        
        /**
         * Ruudun merkkijonomuunnos. Tässä muodossa ruudut kirjoitetaan tiedostoon. Koordinaatteja ei merkitä, koska ne on pääteltävissä rivin järjestysnumerosta.
         * @return ruudun olennaiset tiedot merkkijonona
         */
        @Override
        public String toString() {
            return maankaytto + "|" + katu + "|" + rakennus + "|" + tontti + "|" + e + "|" + String.format("%.3f", korkeus).replace(",", ".") + "|" + rataa;
        }
    }
    
    
    private Ruutu[][] sisalto;
    private int sivu;
    private int tontteja = 0;
    
    
    /**
     * @param sivu ruutujen määrän neliöjuuri
     */
    public Kartta(int sivu) {
        this.sivu = sivu;
        sisalto = new Ruutu[sivu][sivu];
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) sisalto[i][j] = new Ruutu(i, j);
        }
    }
    
    
    /**
     * Muodostaa kartan annetun tiedoston perusteella.
     * @param tiedosto Polku, josta tiedosto löytyy
     * @throws FileNotFoundException Jos tiedostoa ei löydy.
     */
    public Kartta(String tiedosto) throws FileNotFoundException {
        try (Scanner fi = new Scanner(new FileInputStream(tiedosto))) {
            sivu = Integer.parseInt(fi.nextLine());
            sisalto = new Ruutu[sivu][sivu];
            int i = 0;
            while (i < sivu) {
                int j = 0;
                while (j < sivu && fi.hasNext()) {
                    Ruutu ruutu = new Ruutu(i, j);
                    ruutu.parse(fi.next());
                    sisalto[i][j] = ruutu;
                    j++;
                }
                i++;
            }
        }
    }
    
    
    /**
     * Muuttaa kartan ruutujen korkeuksia Perlin-kohinan mukaisesti.
     * @param lahtotarkkuus Moneenko neliön muotoiseen osaan kartta jaetaan pysty- ja vaakasuunnassa aluksi
     * @param maalitarkkuus Moneenko neliön muotoiseeen osaan kartta täytyy olla jaettuna pysty- ja vaakasuunnassa, jotta pienemmän kohinan muodostaminen isomman päälle loppuu. Jos tämä on edellinen/2, kohina lisätään vain yhdessä mittakaavassa.
     * @param maksimiero kuinka voimakasta kohina on
     */
    public void luoKorkeuserot(int lahtotarkkuus, int maalitarkkuus, double maksimiero) {
        for (int ruudunSivu = sivu/lahtotarkkuus; ruudunSivu != 1 && ruudunSivu != sivu/maalitarkkuus; ruudunSivu /= 2) {
            int ruudukonSivu = sivu/ruudunSivu+1;
            Vektori[][] vektorit = new Vektori[ruudukonSivu][ruudukonSivu];
            for (int i = 0; i < ruudukonSivu; i++) {
                for (int j = 0; j < ruudukonSivu; j++) {
                    double suunta = Math.random()*Math.PI*2;
                    vektorit[i][j] = new Vektori(Math.cos(suunta), Math.sin(suunta));
                }
            }
            for (int i = 0; i < ruudukonSivu-1; i++) {
                for (int j = 0; j < ruudukonSivu-1; j++) {
                    Vektori gradientVH = vektorit[i][j];
                    Vektori gradientOH = vektorit[i+1][j];
                    Vektori gradientVP = vektorit[i][j+1];
                    Vektori gradientOP = vektorit[i+1][j+1];
                    for (int k = 0; k < ruudunSivu; k++) {
                        for (int l = 0; l < ruudunSivu; l++) {
                            double vasen = Funktiot.perlinKayra((k + 0.5)/ruudunSivu);
                            double huippu = Funktiot.perlinKayra((l + 0.5)/ruudunSivu);
                            double oikea = vasen - 1;
                            double pohja = huippu - 1;
                            Vektori etaisyysVH = new Vektori(vasen, huippu);
                            Vektori etaisyysOH = new Vektori(oikea, huippu);
                            Vektori etaisyysVP = new Vektori(vasen, pohja);
                            Vektori etaisyysOP = new Vektori(oikea, pohja);
                            double pistetuloVH = etaisyysVH.pistetulo(gradientVH);
                            double pistetuloOH = etaisyysOH.pistetulo(gradientOH);
                            double pistetuloVP = etaisyysVP.pistetulo(gradientVP);
                            double pistetuloOP = etaisyysOP.pistetulo(gradientOP);
                            double nelioVH = vasen*huippu;
                            double nelioOH = -1*oikea*huippu;
                            double nelioVP = -1*vasen*pohja;
                            double nelioOP = oikea*pohja;
                            sisalto[i*ruudunSivu+k][j*ruudunSivu+l].korkeus += (pistetuloVH*nelioOP + pistetuloOH*nelioVP + pistetuloVP*nelioOH + pistetuloOP*nelioVH)*maksimiero*ruudunSivu/sivu;
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * Floodfill-algoritmi.
     * @param i lähtöruudun x-koordinaatti
     * @param j lähtöruudun y-koordinaatti
     * @param kasittely mitä algoritmin alaisille ruuduille tehdään
     * @param ehto mikä ehto ruudun on täytettävä, jotta se menee algoritmin alle
     */
    public void floodfill(int i, int j, FunktioRuutuVoid kasittely, FunktioRuutuBoolean ehto) {
        if (!ehto.f(sisalto[i][j])) return;
        boolean[][] vierailtu = new boolean[sivu][sivu];
        Stack<Ruutu> pino = new Stack<Ruutu>();
        pino.push(sisalto[i][j]);
        while (!pino.empty()) {
            Ruutu s = pino.pop();
            if (vierailtu[s.x][s.y]) continue;
            kasittely.f(s);
            vierailtu[s.x][s.y] = true;
            if (kartalla(s.x-1, s.y) && !vierailtu[s.x-1][s.y] && ehto.f(sisalto[s.x-1][s.y])) pino.push(sisalto[s.x-1][s.y]);
            if (kartalla(s.x+1, s.y) && !vierailtu[s.x+1][s.y] && ehto.f(sisalto[s.x+1][s.y])) pino.push(sisalto[s.x+1][s.y]);
            if (kartalla(s.x, s.y-1) && !vierailtu[s.x][s.y-1] && ehto.f(sisalto[s.x][s.y-1])) pino.push(sisalto[s.x][s.y-1]);
            if (kartalla(s.x, s.y+1) && !vierailtu[s.x][s.y+1] && ehto.f(sisalto[s.x][s.y+1])) pino.push(sisalto[s.x][s.y+1]);
        }
    }
    
    
    /**
     * Tallentaa kartan helposti tämän ohjelman luettavaan tiedostoon.
     * @param tiedosto polku, johon tiedosto kirjoitetaan
     * @throws FileNotFoundException jos tiedostoa ei löydy
     */
    public void kirjoita(String tiedosto) throws FileNotFoundException {
        try (PrintStream fo = new PrintStream(new FileOutputStream(tiedosto))) {
            fo.println(sivu); // Ensimmäisellä rivillä kuvataan kartan sivun pituus ruutuina. Ruutujen koordinaatit päätellään tästä ja niiden järjestyksestä tiedostossa.
            for (int i = 0; i < sivu; i++) {
                for (int j = 0; j < sivu; j++) fo.println(sisalto[i][j]);
            }
        }
    }
    
    
    /**
     * Piirtää kartan PNG-kuvatiedostoon.
     * @param tiedosto polku, johon tiedosto kirjoitetaan
     * @param varit kuvaa värit, joilla kutakin maankäyttötyyppiä merkitään
     * @param tiet Kuvaa värit, joilla kutakin tietyyppiä merkitään. Tässä versiossa teitä on vain yhtä tyyppiä.
     * @param tonttiraja millä värillä tonttirajaa merkitään
     * @param rata millä värillä rautatietä kuvataan
     * @param korkeuskayra millä värillä korkeuskäyrää kuvataan
     */
    public void piirra(String tiedosto, Color[] varit, Color[] tiet, Color tonttiraja, Color rata, Color korkeuskayra) {
        BufferedImage bImg = new BufferedImage(sivu, sivu, BufferedImage.TYPE_INT_RGB);
        Graphics g = (Graphics2D)bImg.getGraphics();
        int kayravali = 5;
        
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
            	if (sisalto[i][j].katu == 0) {
            		g.setColor(varit[sisalto[i][j].maankaytto]);
            		g.fillRect(i,  j, 1, 1);
            	}
            	if (sisalto[i][j].maankaytto == 0 && sisalto[i][j].katu == 0) {
            		// Metsä- tai peltoalueelle piirretään korkeuskäyrät. Ne merkitään 5 metrin välein.
	        		boolean kayraa = false;
	        		for (int k = i - 1; k <= i + 1; k++) {
	        			for (int l = j - 1; l <= j + 1; l++) {
	        				if ((i == k || l == j) && kartalla(k, l) && ((int)sisalto[k][l].korkeus/kayravali < (int)sisalto[i][j].korkeus/kayravali || sisalto[k][l].korkeus < 0 && 0 <= sisalto[i][j].korkeus)) kayraa = true;
	        			}
	        		}
	        		if (!kayraa) continue;
	                g.setColor(korkeuskayra);
	                g.fillRect(i, j, 1, 1);
            	}
				boolean tonttir = false;
        		for (int k = i - 1; k <= i + 1; k++) {
        			for (int l = j - 1; l <= j + 1; l++) {
        				if ((i == k || l == j) && kartalla(k, l) && sisalto[k][l].tontti != 0 && sisalto[i][j].tontti < sisalto[k][l].tontti) tonttir = true;
        			}
        		}
				if (!tonttir) continue;
				g.setColor(tonttiraja);
                g.fillRect(i, j, 1, 1);
            }
        }
        
        for (int i = 0; i < sivu; i++) {
        	for (int j = 0; j < sivu; j++) {
        		if (sisalto[i][j].katu != 0) {
        			g.setColor(tiet[sisalto[i][j].katu]);
        			g.fillRect(i - 1, j - 1, 3, 3);
        		}
        	}
        }
        
        g.setColor(rata);
        for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) {
                if (sisalto[i][j].rataa) g.fillRect(i, j, 1, 1);
                if (sisalto[i][j].rakennus == 1) g.fillRect(i-1, j-1, 3, 3);
            }
        }
        
        // Vasempaan yläreunaan tulee 500 metrin mittatikku.
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("500 m", 20, 30);
        g.drawLine(10, 35, 110, 35);
        
        try {
            if (ImageIO.write(bImg, "png", new File(tiedosto))) {
                System.out.println("Saved");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * Kartan oma indeksitarkistin.
     * @param i x-koordinaatti
     * @param j y-koordinaatti
     * @return onko kartalla koordinaattien mukaista ruutua
     */
    public boolean kartalla(int i, int j) {
        return -1 < i && -1 < j && i < sivu && j < sivu;
    }
    
    
    /**
     * A*-algoritmi.
     * @param lahto ruutu, josta algoritmi aloittaa
     * @param etaisyydet taulukko, johon ruutujen etäisyydet lähdöstä tallennetaan
     * @param edelliset taulukko, johon ruuduista tallennetaan se, mistä ruudusta lyhyin reitti lähdöstä on kyseiseen ruutuun saapunut
     * @param naapurit miten haetaan ruudun naapurit eli ruudut, joihin ruudusta on suora yhteys
     * @param kaari miten kahden ruudun välisen kaaren pituus määritellään
     * @param heuristiikka miten etäisyys ruudusta maaliin arvioidaan
     * @param ehto mikä ehto ruudun on täytettävä, jotta se voidaan hyväksyä maaliksi
     * @return lähdöstä lähin ehdon täyttävä ruutu tai lähdöstä kaukaisin yhteydellinen ruutu, jos mikään yhteydellinen ruutu ei täytä ehtoa
     */
    public Ruutu aTahti(Ruutu lahto, double[][] etaisyydet, Ruutu[][] edelliset, FunktioRuutuRuutulist naapurit, Funktio2RuutuaDouble kaari, FunktioRuutuDouble heuristiikka, FunktioRuutuBoolean ehto) {
        boolean[][] vierailtu = new boolean[sivu][sivu];
        double[][] heuristiikat = new double[sivu][sivu];
    	for (int i = 0; i < sivu; i++) {
            for (int j = 0; j < sivu; j++) etaisyydet[i][j] = Double.POSITIVE_INFINITY;
        }
		Keko<Ruutu> reunat = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y] + heuristiikat[r.x][r.y], r -> r.x*sivu + r.y, sivu*sivu);
        Ruutu t = lahto;
		etaisyydet[t.x][t.y] = 0;
		heuristiikat[t.x][t.y] = heuristiikka.f(t);
		reunat.lisaa(t);
        while (reunat.size() != 0 && !ehto.f(t = reunat.pienin())) {
            vierailtu[t.x][t.y] = true;
            for (Ruutu n : naapurit.f(t)) {
            	int i = n.x;
            	int j = n.y;
            	if (vierailtu[i][j]) continue;
            	double k = etaisyydet[t.x][t.y] + kaari.f(t, n);
            	if (etaisyydet[i][j] <= k) continue;
            	boolean uusi = etaisyydet[i][j] == Double.POSITIVE_INFINITY;
            	etaisyydet[i][j] = k;
            	edelliset[i][j] = t;
            	if (uusi) {
            		heuristiikat[i][j] = heuristiikka.f(sisalto[i][j]);
            		reunat.lisaa(n);
            	} else reunat.nosta(n);
            }
        }
        return t;
    }
    
    
    /**
     * Dijkstran algoritmi käsitellään A*-algoritmina, jossa heuristiikkafunktio palauttaa aina nollan.
     * @param lahto ruutu, josta algoritmi aloittaa
     * @param etaisyydet taulukko, johon ruutujen etäisyydet lähdöstä tallennetaan
     * @param edelliset taulukko, johon ruuduista tallennetaan se, mistä ruudusta lyhyin reitti lähdöstä on kyseiseen ruutuun saapunut
     * @param naapurit miten haetaan ruudun naapurit eli ruudut, joihin ruudusta on suora yhteys
     * @param kaari miten kahden ruudun välisen kaaren pituus määritellään
     * @param ehto mikä ehto ruudun on täytettävä, jotta se voidaan hyväksyä maaliksi
     * @return lähdöstä lähin ehdon täyttävä ruutu tai lähdöstä kaukaisin yhteydellinen ruutu, jos mikään yhteydellinen ruutu ei täytä ehtoa
     */
    public Ruutu dijkstra(Ruutu lahto, double[][] etaisyydet, Ruutu[][] edelliset, FunktioRuutuRuutulist sade, Funktio2RuutuaDouble kaari, FunktioRuutuBoolean ehto) {
    	return aTahti(lahto, etaisyydet, edelliset, sade, kaari, r -> 0, ehto);
     }
    
    
    /**
     * Etsii annetusta ruudusta linnuntietä lähimmän tietyn ehdon täyttävän ruudun.
     * @param lahto ruutu, josta etsintä alkaa
     * @param nakyvyys säde, jolta ehdon täyttävää ruutua etsitään
     * @param ehto mikä ehto etsittävän ruudun on täytettävä
     * @return linnuntietä lähin ehdon täyttävä ruutu tai null, jos annetulta säteeltä ei löydy ehdon täyttävää ruutua
     */
    public Ruutu tutka(Ruutu lahto, int nakyvyys, FunktioRuutuBoolean ehto) {
    	for (int r = 0; r < nakyvyys; r++) {
    		int r2max = (r + 1)*(r + 1);
    		int rmin = r*r;
    		int x = 0;
    		int y = r;
    		while (x <= y) {
    			for (int i = -1; i < 2; i += 2) {
    				for (int j = -1; j < 2; j += 2) {
    					if (kartalla(lahto.x+i*x, lahto.y+j*y) && ehto.f(sisalto[lahto.x+x*i][lahto.y+y*j])) return sisalto[lahto.x+x*i][lahto.y+y*j];
    					if (kartalla(lahto.x+j*x, lahto.y+i*y) && ehto.f(sisalto[lahto.x+x*j][lahto.y+y*i])) return sisalto[lahto.x+x*j][lahto.y+y*i];
    				}
    			}
    			if (rmin <= x*x + (y-1)*(y-1)) y--;
    			else {
    				x++;
    				if (r2max <= x*x + y*y) y--;
    			}
    		}
    	}
    	return null;
    }
    
    
    /**
     * Bresenhamin jana-algoritmi. Jos lasketuilla koordinaateilla ei ole vastaavaa ruutua, algoritmi sivuuttaa ne.
     * @param x1 toisen janan päätepisteen X-koordinaatti
     * @param y1 toisen janan päätepisteen y-koordinaatti
     * @param x2 toisen janan päätepisteen x-koordinaatti
     * @param y2 toisen janan päätepisteen y-koordinaatti
     * @param kasittely mitä koordinaatteja vastaavalle ruudulle tehdään, jos sellainen on olemassa
     */
    public void bresenham(int x1, int y1, int x2, int y2, FunktioRuutuVoid kasittely) {
        int minx = Math.min(x1, x2);
        int miny = Math.min(y1, y2);
        int maxx = Math.max(x1, x2);
        int maxy = Math.max(y1, y2);
        if (maxy - miny < maxx - minx) {
    		double d = 1.0*(y1 - y2)/(maxx - minx);
    		int ly = y2;
        	if (minx == x1) {
        		d *= -1;
        		ly = y1;
        	}
            for (int i = minx; i <= maxx; i++) {
                int j = (int)(d*(i-minx))+ly;
                if (kartalla(i, j)) kasittely.f(sisalto[i][j]);
            }
        } else {
    		double d = 1.0*(x1 - x2)/(maxy - miny);
    		int lx = x2;
        	if (miny == y1) {
        		d *= -1;
        		lx = x1;
        	}
            for (int j = miny; j <= maxy; j++) {
                int i = (int)(d*(j-miny))+lx;
                if (kartalla(i, j)) kasittely.f(sisalto[i][j]);
            }
        }
    }

    
    /**
     * Edellinen toisessa muodossa. Koordinaattien sijaan annetaan niitä vastaavat ruudut.
     * @param a toinen janan päätepiste
     * @param b toinen janan päätepiste
     * @param kasittely mitä janalla oleville ruuduille tehdään
     */
    public void bresenham(Ruutu a, Ruutu b, FunktioRuutuVoid kasittely) {
        bresenham(a.x, a.y, b.x, b.y, kasittely);
    }
  
    
    /**
     * Jakaa korttelin tontteihin rekursiivisesti. Menetelmä ei ole täysin itse keksitty.
     * @param alue alue, jota (mahdollisesti) puolitettava alue koskee
     * @param eRaja Tehokkuusluku, jonka ylittäessään alueesta tulee kerrostaloalue, jos se muuten olisi pientaloalue. Tehokkuusluku on alueen lattiapinta-ala jaettuna maapinta-alalla.
     * @param koot Kutakin maankayttötyyppiä vastaava tonttikoon maksimi. Jos alueen pinta-ala alittaa tämän, alueesta muododstetaan tontti.
     */
    public void puolita(ArrayList<Ruutu> alue, double eRaja, int[] koot) {
    	if (alue.size() == 0) return;
    	// Jos alue ei ole yhtenäinen, käsitellään osat erillään.
    	ArrayList<Ruutu> jako1 = new ArrayList<Ruutu>();
    	ArrayList<Ruutu> jako2 = new ArrayList<Ruutu>();
    	jako2.addAll(alue);
    	floodfill(jako2.get(0).x, jako2.get(0).y, r -> {
    		jako2.remove(r);
    		jako1.add(r);
    	}, r -> alue.contains(r));
    	if (jako2.size() != 0) {
    		puolita(jako2, eRaja, koot);
    		puolita(jako1, eRaja, koot);
    		return;
    	}
    	// Selvitetään käsiteltävän alueen yleisin maankäyttötyyppi ja lattiapinta-alan määrä. Jos alueen pinta-ala on alle tyyppiä vastaavan kokorajan, lopetetaan jakaminen ja muodostetaan alueesta tontti.
    	int[] yleisyydet = new int[koot.length];
    	int e = 0;
    	for (Ruutu r : jako1) {
    		e += r.e;
    		yleisyydet[r.maankaytto]++;
    	}
    	int yleisin = yleisyydet[0];
    	for (int i = 1; i < koot.length; i++) {
    		if (yleisyydet[yleisin] < yleisyydet[i]) yleisin = i;
    	}
    	if (yleisin == 2 && eRaja < 1.0*e/jako1.size()) yleisin = 4;
    	if (jako1.size() <= koot[yleisin]) {
    		luoTontti(alue, yleisin);
    		return;
    	}
    	// Etsitään raa'alla voimalla ainakin melkein pienin suorakulmio, jonka sisään alue mahtuu. Apuna on kääntömatriisi.
    	ArrayList<Ruutu> reunat = new ArrayList<Ruutu>();
    	reunat.addAll(jako1);
    	ArrayList<int[]> sade = new ArrayList<int[]>();
    	sade.add(new int[] {-1,0});
    	sade.add(new int[] {1,0});
    	sade.add(new int[] {0,-1});
    	sade.add(new int[] {0,1});
    	for (int i = reunat.size() - 1; i >= 0; i--) {
    		boolean reunalla = false;
    		for (int[] suunta : sade) reunalla = reunalla || kartalla(reunat.get(i).x + suunta[0], reunat.get(i).y + suunta[1]) && !jako1.contains(sisalto[reunat.get(i).x+suunta[0]][reunat.get(i).y+suunta[1]]);
    		if (!reunalla) reunat.remove(i);
    	}
    	Ruutu keskus = reunat.get(0);
    	double ennatys = Double.POSITIVE_INFINITY;
    	double parasKulma = 0;
    	double[] karjet = new double[4];
    	for (double x = 0; x < Math.PI/2; x += Math.PI/32) {
    		double cos = Math.cos(x);
    		double sin = Math.sin(x);
        	double maxx = cos*keskus.x - sin*keskus.y;
        	double minx = maxx;
        	double maxy = sin*keskus.x + cos*keskus.y;
        	double miny = maxy;
        	for (int i = 1; i < reunat.size(); i++) {
        		double x2 = cos*reunat.get(i).x - sin*reunat.get(i).y;
        		double y2 = sin*reunat.get(i).x + cos*reunat.get(i).y;
        		maxx = Math.max(maxx, x2);
        		minx = Math.min(minx, x2);
        		maxy = Math.max(maxy, y2);
        		miny = Math.min(miny, y2);
        	}
    		if (ennatys <= (maxx - minx)*(maxy - miny)) continue;
    		ennatys = (maxx - minx)*(maxy - miny);
    		parasKulma = x;
    		karjet = new double[] {maxx, minx, maxy, miny};
    	}
    	double cos = Math.cos(-1*parasKulma);
    	double sin = Math.sin(-1*parasKulma);
    	// Alue puolitetaan löytyneen suorakulmion mukaan siten, että jakolinja on suorakulmion lyhyempien sivujen kanssa yhdensuuntainen ja niiden puolivälissä.
		Ruutu kulma1, kulma2, kulma3, kulma4;
        kulma1 = sisalto[(int)(cos*karjet[0] - sin*karjet[2])][(int)(sin*karjet[0] + cos*karjet[2])];
        kulma2 = sisalto[(int)(cos*karjet[0] - sin*karjet[3])][(int)(sin*karjet[0] + cos*karjet[3])];
        kulma3 = sisalto[(int)(cos*karjet[1] - sin*karjet[2])][(int)(sin*karjet[1] + cos*karjet[2])];
        kulma4 = sisalto[(int)(cos*karjet[1] - sin*karjet[3])][(int)(sin*karjet[1] + cos*karjet[3])];
        Ruutu linja1 = keskus(kulma1, kulma2);
        Ruutu linja2 = keskus(kulma3, kulma4);
        if (etaisyys2(linja1, linja2) < etaisyys2(keskus(kulma1, kulma3), keskus(kulma2, kulma4))) {
        	linja1 = keskus(kulma1, kulma3);
        	linja2 = keskus(kulma2, kulma4);
        }
        ArrayList<Ruutu> osa1 = new ArrayList<Ruutu>();
        ArrayList<Ruutu> osa2 = new ArrayList<Ruutu>();
    	for (Ruutu r : jako1) {
    		if (etaisyys2(r, linja1) < etaisyys2(r, linja2)) osa1.add(r);
    		else osa2.add(r);
    	}
    	// Jos jommallakummalla puolituksen tuloksista ei ole katuyhteyttä, ei puolitusta suoritetakaan.
    	boolean katuyhteys = false;
    	for (Ruutu r : osa1) {
    		for (int[] suunta : sade) katuyhteys = katuyhteys || kartalla(r.x+suunta[0],r.y+suunta[1]) && sisalto[r.x+suunta[0]][r.y+suunta[1]].katu != 0;
    	}
    	if (!katuyhteys) {
    		luoTontti(jako1, yleisin);
    		return;
    	}
    	katuyhteys = false;
    	for (Ruutu r : osa2) {
    		for (int[] suunta : sade) katuyhteys = katuyhteys || kartalla(r.x+suunta[0],r.y+suunta[1]) && sisalto[r.x+suunta[0]][r.y+suunta[1]].katu != 0;
    	}
    	if (!katuyhteys) {
    		luoTontti(jako1, yleisin);
    		return;
    	}
    	// Jos päästiin tänne asti, jatketaan tonttien jakoa rekursiivisesti.
    	puolita(osa1, eRaja, koot);
    	puolita(osa2, eRaja, koot);
    }
    
    
    /**
     * Luo annetusta ruutulistasta oman tonttinsa. Tontin maankäytön on oltava yhtenäinen.
     * @param alue ruudut, jotka kuuluvat muodostettavaan tonttiin
     * @param tyyppi maankäyttötyyppi, jota tontti edustaa
     */
    public void luoTontti(ArrayList<Ruutu> alue, int tyyppi) {
    	int nro = ++tontteja;
    	for (Ruutu r : alue) {
    		r.maankaytto = tyyppi;
    		r.tontti = nro;
    	}
    }
        
    
    /**
     * Muodostaa karttaan tien tai vastaavan väylän annettujen lähtö- ja maaliruutujen ja viitetaulukon avulla. Käytetään usein A*-algoritmin jälkeen.
     * @param lahto ruutu, josta väylä alkaa
     * @param maali ruutu, johon väylä päättyy
     * @param viitteet taulukko, joka kertoo ruudusta, mihin ruutuun väylä seuraavaksi siirtyy
     * @param millainen mitä väylällä oleville ruuduille tehdään
     */
    public void luoTie(Ruutu lahto, Ruutu maali, Ruutu[][] viitteet, FunktioRuutuVoid millainen) {
    	Ruutu a = lahto;
    	while (a != maali) {
    		Ruutu b = viitteet[a.x][a.y];
    		bresenham(a, b, millainen);
    		a = b;
    	}
    }
    
    
    /**
     * @param a toinen ruutu
     * @param b toinen ruutu
     * @return Ruutujen etäisyyden neliö. Etäisyys voidaan laskea neliöjuuresta.
     */
    public static int etaisyys2(Ruutu a, Ruutu b) {
    	int dx = a.x - b.x;
    	int dy = a.y - b.y;
    	return dx*dx + dy*dy;
    }
    
    
    /**
     * @param a janan toinen pääteruutu
     * @param b janan toinen pääteruutu
     * @return ruutujen koordinaattien keskiarvoja vastaava ruutu
     */
    public Ruutu keskus(Ruutu a, Ruutu b) {
    	return sisalto[(a.x + b.x)/2][(a.y + b.y)/2];
    }
    

    /**
     * Pääohjelma. Generointiprosessin mielekkyys on ehkä kyseenalainen, mutta prosessi toimii esimerkkinä 
     * muun ohjelmakoodin käytöstä.
     * @param args ei käytössä
     * @throws FileNotFoundException jos tiedoston luku tai kirjoitus ei onnistu
     */
    public static void main(String[] args) throws FileNotFoundException {
    	// perustiedot
    	int n = 512;
        Kartta map = new Kartta(n);
        
        Funktio2RuutuaDouble katu = (r1, r2) -> {
        	double e = Math.sqrt(etaisyys2(r1, r2));
        	if (r2.maankaytto != 1) e += Math.abs(r1.korkeus - r2.korkeus)*16;
        	return e;
        };
        
        // Luonnonmaantiede määritellään alussa. Keskustan vierestä virtaa joki, jonka uoma perustuu yhteen Perlin-kohinaan. Korkeuserot joen eri puolilla perustuvat kahteen eri Perlin-kohinaan.
        double h = 64;
        double kulma = Math.random()*Math.PI*2;
        double cos = Math.cos(kulma);
        double sin = Math.sin(kulma);
        for (int i = 0; i <  n; i++) {
        	for (int j = 0; j < n; j++) map.sisalto[i][j].korkeus = h*(cos*(i - n/2) - sin*(j - n/2))/(n/2);
        }
        map.luoKorkeuserot(2, 4, h);
        int jokeen = 60;
        int jokix = n/2 - (int)(cos*jokeen);
        int jokiy = n/2 + (int)(sin*jokeen);
        double nolla = map.sisalto[jokix][jokiy].korkeus;
        for (int i = 0; i <  n; i++) {
        	for (int j = 0; j < n; j++) {
        		map.sisalto[i][j].korkeus = Math.abs(map.sisalto[i][j].korkeus - nolla);
        	}
        }
        Kartta korkeudet1 = new Kartta(n);
        Kartta korkeudet2 = new Kartta(n);
        korkeudet1.luoKorkeuserot(2, n, 2);
        korkeudet2.luoKorkeuserot(2, n, 2);
        boolean[][] jokipuoli = new boolean[n][n];
        double joki = 2;
        map.floodfill(jokix, jokiy, r -> r.maankaytto = 1, r -> r.korkeus < joki);
        map.floodfill(n/2, n/2, r -> jokipuoli[r.x][r.y] = true, r -> r.maankaytto == 0);
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		korkeudet1.sisalto[i][j].korkeus *= korkeudet1.sisalto[i][j].korkeus;
        		korkeudet2.sisalto[i][j].korkeus *= korkeudet2.sisalto[i][j].korkeus;
        		if (map.sisalto[i][j].maankaytto == 1) continue;
        		if (jokipuoli[i][j]) map.sisalto[i][j].korkeus = joki + (map.sisalto[i][j].korkeus - joki)*korkeudet1.sisalto[i][j].korkeus;
        		else map.sisalto[i][j].korkeus = joki + (map.sisalto[i][j].korkeus - joki)*korkeudet2.sisalto[i][j].korkeus;
        	}
        }
                
        // rakennetun alueen suuripiirteisten rajojen määritys
        ArrayList<int[]> katukeha = new ArrayList<int[]>();
        for (int i = 1; i < 4; i++) katukeha.addAll(Funktiot.sadekeha(i));
        FunktioRuutuRuutulist katunaapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : katukeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x+suunta[0]][r.y+suunta[1]]);
        	}
        	return palaute;
        };
        double[][] etaisyydet = new double[n][n];
        Ruutu[][] edelliset = new Ruutu[n][n];
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet, edelliset, katunaapurit, katu, r -> false);
        Ruutu[] etaisyydet2 = new Ruutu[n*n];
        for (int i = 0; i < n*n; i++) etaisyydet2[i] = map.sisalto[i/n][i%n];
        Keko<Ruutu> etaisyyskeko = new Keko<Ruutu>(r -> etaisyydet[r.x][r.y], etaisyydet2);
        int maata = 0;
        while (etaisyyskeko.size() != 0) {
        	Ruutu s = etaisyyskeko.pienin();
        	if (s.maankaytto == 0) etaisyydet2[maata++] = s;
        }
        
       // Muuttuja sdn kuvaa, montako keskipisteestä lähintä ruutua on alle keskihajonnan päässä keskustasta 
       // kadunkuljettavaa reittiä pitkin. Keskihajonta on rakennuskannan keskimääräinen etäisyys keskustasta. 
       // Käytännössä tämä määrää, miten pienelle alueelle kaupunki keskittyy. 
        final int sdn = 5000;
        final double sd = etaisyydet[etaisyydet2[sdn].x][etaisyydet2[sdn].y];
        final Ruutu[] reunat = new Ruutu[(n - 1)*4];
        for (int i = 0; i < n - 1; i++) {
        	reunat[i] = map.sisalto[i][0];
        	reunat[(n - 1) + i] = map.sisalto[0][i + 1];
        	reunat[(n - 1)*2 + i] = map.sisalto[i][n - 1];
        	reunat[(n - 1)*3 + i] = map.sisalto[n - 1][i + 1];
        }
        
        // Rautatien linja etsitään raa'alla voimalla. Se lähtee siitä kohdasta reunaa, 
        // josta rata alle keskihajonnan päähän keskustasta on halvimmillaan rakennettavissa.
        // Rata jatkuu tästä sinne lähelle reunaa, jonne se on halvimmillaan jatkettavissa.
    	final Ruutu[][] rataedelliset = new Ruutu[n][n];
        int ratapituus = 8;
        Funktio2RuutuaDoubleDouble ratakaari = (r1, r2, he) -> {
        	Ruutu edellinen = rataedelliset[r1.x][r1.y];
        	double kaarre = 0;
        	if (edellinen != null) {
            	int dx = (r2.x - r1.x) - (r1.x - edellinen.x);
            	int dy = (r2.y - r1.y) - (r1.y - edellinen.y);
            	kaarre = dx*dx + dy*dy;
        	}
        	double e = kaarre + Math.abs(he - r2.korkeus);
        	if (etaisyydet[r2.x][r2.y] < sd || r2.maankaytto == 1) e *= 4; 
        	return e;
        };
        ArrayList<int[]> ratakeha = Funktiot.sadekeha(ratapituus);
        FunktioRuutuRuutulist ratanaapurit = r -> {
        	ArrayList<Ruutu> palaute = new ArrayList<Ruutu>();
        	for (int[] suunta : ratakeha) {
        		if (map.kartalla(r.x + suunta[0], r.y + suunta[1])) palaute.add(map.sisalto[r.x+suunta[0]][r.y+suunta[1]]);
        	}
        	return palaute;
        };
        double ennatys = Double.POSITIVE_INFINITY;
        FunktioRuutuBoolean lahellaReunaa = r -> r.x < ratapituus || r.y < ratapituus || n - ratapituus - 1 < r.x || n - ratapituus - 1 < r.y;
        Ruutu[][] parasReitti = new Ruutu[n][n];
        Ruutu asema = null;
        Ruutu lahto = null;
        for (Ruutu reuna : reunat) {
        	if ((reuna.x + reuna.y) % 10 != 0) continue;
        	System.out.println(reuna.x+" "+reuna.y);
            final double korkeus = reuna.korkeus;
            double[][] rataetaisyydet = new double[n][n];
            for (int i = 0; i < n; i++) {
            	for (int j = 0; j < n; j++) rataedelliset[i][j] = null;
            }
            final double finalEnnatys = ennatys;
            Ruutu rata1 = map.dijkstra(reuna, rataetaisyydet, rataedelliset, ratanaapurit, (r1, r2) -> ratakaari.f(r1, r2, korkeus), r -> r.maankaytto != 1 && etaisyydet[r.x][r.y] < sd || finalEnnatys <= rataetaisyydet[r.x][r.y]);
            if (ennatys <= rataetaisyydet[rata1.x][rata1.y]) continue;
            ennatys = rataetaisyydet[rata1.x][rata1.y];
            asema = rata1;
            lahto = reuna;
            for (int i = 0; i < n; i++) {
            	for (int j = 0; j < n; j++) parasReitti[i][j] = rataedelliset[i][j];
            }
            System.out.println(ennatys);
        }
        asema.rakennus = 1;
        System.out.println("!");
        map.luoTie(asema, lahto, parasReitti, r -> r.rataa = true);
        final double asemakorkeus = asema.korkeus;
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) rataedelliset[i][j] = null;
        }
        rataedelliset[asema.x][asema.y] = parasReitti[asema.x][asema.y];
        Ruutu lahto1 = map.dijkstra(asema, new double[n][n], rataedelliset, ratanaapurit, (r1, r2) -> ratakaari.f(r1, r2, asemakorkeus), lahellaReunaa);
        map.luoTie(lahto1, asema, rataedelliset, r -> r.rataa = true);
        
        // Muuttuja kohteita määrää rakennuskannan koon. Se sirotellaan kartalle normaalijakauman mukaan.
        ArrayList<Ruutu> rakennukset = new ArrayList<Ruutu>();
        final int kohteita = 5000;
        while (rakennukset.size() < kohteita) {
        	for (int i = 0; i < n; i++) {
        		for (int j = 0; j < n; j++) {
        			if (map.sisalto[i][j].maankaytto != 1 && Math.random() < Funktiot.gauss(etaisyydet[i][j]/sd) && !map.sisalto[i][j].rataa) {
        				rakennukset.add(map.sisalto[i][j]);
        			}
        		}
        	}
        }
        int rakennuskoko = 4;
        Collections.shuffle(rakennukset);
        for (int i = rakennukset.size() - 1; i >= kohteita; i--) rakennukset.remove(i);
        final int tonttileveys = 2;
        for (Ruutu r : rakennukset) {
        	r.e += rakennuskoko;
    		for (int i = r.x - tonttileveys; i <= r.x + tonttileveys; i++) {
    			for (int j = r.y - tonttileveys; j <= r.y + tonttileveys; j++) {
    				if (map.kartalla(i, j) && map.sisalto[i][j].maankaytto != 1) map.sisalto[i][j].maankaytto = 2;
    			}
    		}
        }
        
        // Teollisuusalue määritellään muodostamalla rakennetuista ruuduista keko, jonka prioriteetti kuvaa ruudun sopivuutta teollisuudelle.
        // Keosta valitaan yksi kerrallaan sopivin ruutu, kunnes teollisuusalueen pinta-ala on yli 10 % rakennusalueesta.
        ArrayList<Ruutu> teollisuusehdokkaat = new ArrayList<Ruutu>();
        boolean[][] keossa = new boolean[n][n];
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		if (1 < map.sisalto[i][j].maankaytto) {
        			teollisuusehdokkaat.add(map.sisalto[i][j]);
        			keossa[i][j] = true;
        		}
        	}
        }
        
        // "Renkaaseen" kuvaa etäisyyttä rakennuskannan keskihajonnasta. Idea on, että tämä on teollisuudelle sopivin vyöhyke.
        double[][] renkaaseen = new double[n][n];
        // "Rinteisyys" kuvaa ruudun ja lähistön ruutujen korkeusvaihteluita.
        double[][] rinteisyys = new double[n][n];
        // "Rataan" kuvaa etäisyyttä rautatiestä linnuntietä.
        double[][] rataan = new double[n][n];
        // "Teollisuutta" kuvaa teollisuusruutujen määrää ruudun lähistöllä. Alussa tämä on tosin 1, koska nollalla ei voi jakaa.
        // Taulukko elää koko teollisuusalueen määrityksen ajan.
        int[][] teollisuutta = new int[n][n];
        int rinnetta = 5;
        int maxteollisuus = 10;
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		renkaaseen[i][j] = etaisyydet[i][j]*etaisyydet[i][j]/sd/sd*Funktiot.gauss(etaisyydet[i][j]/sd);
        		double rinteet = 0;
        		for (int k = i - rinnetta; k <= i + rinnetta; k++) {
        			for (int l = j - rinnetta; l <= j + rinnetta; l++) {
        				if ((k - i)*(k - i) + (l - j)*(l - j) <= rinnetta*rinnetta && map.kartalla(k, l) && map.sisalto[i][j].maankaytto != 1) rinteet += Math.abs(map.sisalto[i][j].korkeus - map.sisalto[k][l].korkeus);
        			}
        		}
        		rinteisyys[i][j] = rinteet;
        		rataan[i][j] = Double.POSITIVE_INFINITY;
        		teollisuutta[i][j] = 1;
        	}
        }
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		if (!map.sisalto[i][j].rataa) continue;
        		for (int k = 0; k < n; k++) {
        			for (int l = 0; l < n; l++) rataan[k][l] = Math.min(rataan[k][l], Math.sqrt((k - i)*(k - i) + (l - j)*(l - j)));
        		}
        	}
        }
        
        Keko<Ruutu> teollisuuskeko = new Keko<Ruutu>(r -> rinteisyys[r.x][r.y]*rataan[r.x][r.y]/renkaaseen[r.x][r.y]/teollisuutta[r.x][r.y], teollisuusehdokkaat, r -> r.x*n + r.y, n*n);
        for (int k = 0; k < teollisuusehdokkaat.size()/10; k++) {
        	Ruutu s = teollisuuskeko.pienin();
        	s.maankaytto = 3;
        	keossa[s.x][s.y] = false;
        	for (int i = s.x - maxteollisuus; i <= s.x + maxteollisuus; i++) {
        		for (int j = s.y - maxteollisuus; j <= s.y + maxteollisuus; j++) {
        			if ((i - s.x)*(i - s.x) + (j - s.y)*(j - s.y) <= maxteollisuus*maxteollisuus && map.kartalla(i, j) && keossa[i][j]) {
        				if ((i - s.x)*(i - s.x) + (j - s.y)*(j - s.y) <= maxteollisuus*maxteollisuus) {
        					teollisuutta[i][j]++;
        					teollisuuskeko.nosta(map.sisalto[i][j]);
        				}
        			}
        		}
        	}
        }
        
        // pääsillan määritys
        Ruutu silta1 = etaisyydet2[0];
        for (int i = 0; edelliset[silta1.x][silta1.y] == null || edelliset[silta1.x][silta1.y].maankaytto != 1 || silta1.maankaytto == 1; i++) silta1 = etaisyydet2[i];
        Ruutu silta2 = edelliset[silta1.x][silta1.y];
        while (silta2.maankaytto == 1) silta2 = edelliset[silta2.x][silta2.y];
        map.bresenham(silta1, silta2, r -> r.katu = 1);
        
        // Ulosmenoväylien määrityksessä vaaditaan, että kukin väylistä lähtee kaupungista muista eroavaan suuntaan.
        // Väylät eivät yllä alle keskihajonnan päähän keskustasta.
        Funktio2RuutuaDouble katu2 = (r1, r2) -> {
        	double e = katu.f(r1, r2);
        	final int[] minKatu = new int[] {1};
        	final boolean[] vedessa = new boolean[1];
        	final boolean[] radalla = new boolean[1];
        	map.bresenham(r1, r2, r -> {
        		minKatu[0] = Math.min(minKatu[0], r.katu);
        		vedessa[0] = vedessa[0] || r.maankaytto == 1;
        		radalla[0] = radalla[0] || rataan[r.x][r.y] < 1.1;
        	});
        	if (minKatu[0] == 1) e /= 3;
        	else {
        		if (vedessa[0]) e *= 8;
        		if (radalla[0]) e *= 8;
        	}
        	return e;
        };
        double[][] etaisyydet3 = new double[n][n];
        map.dijkstra(map.sisalto[n/2][n/2], etaisyydet3, edelliset, katunaapurit, katu2, r -> false);
        Keko<Ruutu> uloskeko = new Keko<Ruutu>(r -> etaisyydet3[r.x][r.y], reunat);
        final int ulosmenoja = 4;
        Ruutu[] u = new Ruutu[ulosmenoja];
        for (int i = 0; i < ulosmenoja; i++) {
    		boolean omallaSuunnalla = true;
        	do {
        		u[i] = uloskeko.pienin();
        		omallaSuunnalla = true;
        		for (int j = 0; j < i; j++) omallaSuunnalla = omallaSuunnalla && 1 < Funktiot.kulma(n/2, n/2, u[i].x, u[i].y, u[j].x, u[j].y);
        	} while (!omallaSuunnalla);
        }
        
        for (int i = 0; i < ulosmenoja; i++) map.luoTie(u[i], map.sisalto[n/2][n/2], edelliset, r -> {
        	if (sd < etaisyydet[r.x][r.y]) r.katu = 1;
        });
                
        // Katuverkko lasketaan parittamalla rakennusyksiköt satunnaisesti ja muodostamalla pareista yksi kerrallaan 
        // kadut, jotka näitä pareja parhaiten yhdistävät. Kadun ei tarvitse yltää aivan yksikköön asti, jos lähistöllä on jo katu.
        // Reittejä laskettaessa olemassaolevaa katua suositaan.
        ArrayList<Ruutu> katukohteet = new ArrayList<Ruutu>();
        katukohteet.addAll(rakennukset);
        Collections.shuffle(katukohteet);
        int[] korttelit = new int[] {0,0,12,18,12};
        for (int i = 0; i < katukohteet.size() - 1; i += 2) {
        	if (i % 100 == 0) System.out.println(i+"/"+katukohteet.size());
        	lahto = katukohteet.get(i);
        	Ruutu lahinKatu = map.tutka(lahto, korttelit[lahto.maankaytto], r -> r.katu != 0);
        	if (lahinKatu != null) lahto = lahinKatu;
        	final Ruutu finalLahto = lahto;
        	Ruutu maali = katukohteet.get(i + 1);
        	lahinKatu = map.tutka(maali, korttelit[maali.maankaytto], r -> r.katu != 0);
        	if (lahinKatu != null) maali = lahinKatu;
        	final Ruutu finalMaali = maali;
            Ruutu[][] edelliset2  = new Ruutu[n][n];
        	Ruutu m = map.aTahti(lahto, new double[n][n], edelliset2, katunaapurit, katu2, r -> katu.f(r, finalMaali)/3, r -> r == finalMaali);
        	map.luoTie(m, lahto, edelliset2, r -> {
        		if (tonttileveys*tonttileveys < etaisyys2(r, finalLahto) && tonttileveys*tonttileveys < etaisyys2(r, finalMaali)) r.katu = 1;
        	});
        }
        
        // Puistokorttelit valitaan satunnaisesti niistä, joiden satunnaisesti valittu edustajaruutu on alle 2 keskihajonnan päästä keskustasta.
        ArrayList<ArrayList<Ruutu>> korttelit2 = new ArrayList<ArrayList<Ruutu>>();
        final boolean[][] kasitelty2 = new boolean[n][n];
        int pintaala = 0;
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		map.sisalto[i][j].tontti = 0;
        		if (map.sisalto[i][j].maankaytto < 2 || map.sisalto[i][j].katu != 0 || kasitelty2[i][j] || map.sisalto[i][j].maankaytto == 5) continue;
        		ArrayList<Ruutu> kortteli = new ArrayList<Ruutu>();
        		map.floodfill(i, j, r -> {
        			kasitelty2[r.x][r.y] = true;
        			kortteli.add(r);
        		}, r -> 1 < r.maankaytto && r.katu == 0 && !r.rataa && r.maankaytto != 5);
        		korttelit2.add(kortteli);
        		pintaala += kortteli.size();
        	}
        }
        
        Collections.shuffle(korttelit2);
        int puistoa = 0;
        for (int i = korttelit2.size() - 1; i >= 0 && puistoa < pintaala/10; i--) {
        	ArrayList<Ruutu> kortteli = korttelit2.get(i);
        	boolean keskella = true;
        	for (Ruutu r : kortteli) {
        		if (sd*2 < etaisyydet[r.x][r.y]) {
        			keskella = false;
        			break;
        		}
        	}
        	if (!keskella) continue;
        	for (Ruutu r : kortteli) r.maankaytto = 5;
        	korttelit2.remove(i);
        	puistoa += kortteli.size();
        }
        
        // Puiston läpäisevät kadunpätkät poistetaan.
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < n; j++) {
        		if (map.sisalto[i][j].katu != 1) continue;
        		boolean puistossa = false;
        		if (map.kartalla(i-1, j) && map.kartalla(i+1, j) && map.sisalto[i-1][j].maankaytto == 5 && map.sisalto[i+1][j].maankaytto == 5) puistossa = true;
        		if (map.kartalla(i, j-1) && map.kartalla(i, j+1) && map.sisalto[i][j-1].maankaytto == 5 && map.sisalto[i][j+1].maankaytto == 5) puistossa = true;
        		if (map.kartalla(i-1, j-1) && map.kartalla(i+1, j+1) && map.sisalto[i-1][j-1].maankaytto == 5 && map.sisalto[i+1][j+1].maankaytto == 5) puistossa = true;
        		if (map.kartalla(i-1, j+1) && map.kartalla(i+1, j-1) && map.sisalto[i-1][j+1].maankaytto == 5 && map.sisalto[i+1][j-1].maankaytto == 5) puistossa = true;
        		if (puistossa) {
        			map.sisalto[i][j].katu = 0;
        			map.sisalto[i][j].maankaytto = 5;
        		}
        	}
        }
     
        
        // Puistottomat korttelit jaetaan tontteihin.
        for (ArrayList<Ruutu> kortteli : korttelit2) {
        	map.puolita(kortteli, 0.5, new int[] {0,0,40,200,100});
        }
        
        // Kartta yksilöidään valmistumisajankohtansa mukaan.
        String pvm = new SimpleDateFormat("ddMMyyHHmm").format(new Date());
        map.piirra("/home/ilari-perus/kaupungit/kuvat/"+pvm+".png", new Color[] {Color.green, Color.blue, Color.pink, Color.gray, Color.orange, Color.green, Color.red}, new Color[] {null, Color.white}, Color.red, Color.black, new Color(102,51,0));
      //  map.kirjoita("/home/ilari-perus/kaupungit/tietokannat/"+pvm+".dat");
    }
}