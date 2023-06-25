package org.dominion.dynmap;

public class Util {

public static int distance(int x1,int z1,int x2,int z2){
    return (int) Math.sqrt(Math.pow(x1-x2,2)+Math.pow(z1-z2,2));
}

}
