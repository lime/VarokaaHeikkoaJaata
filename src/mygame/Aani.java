/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

/**
 *
 * @author Petri
 */
public enum Aani {
    KAVELY, HATA, VAROITUS, GANGNAM;
    
    public String annaAani(){
        String s = null;
        switch (this){
            case KAVELY: s = "Sounds/kavely.wav"; break;
            case HATA: s = "Sounds/hata.wav"; break;
            case VAROITUS: s = "Sounds/varoitus.wav"; break;
            case GANGNAM: s = "Sounds/gangnam.wav"; break;
        }
        return s;
    }
    
}
