package org.dominion.dynmap;

import org.dominion.Main;
import org.dominion.logging.ConsoleColors;
import org.dominion.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;

public class DynmapParser {

    private URL url;
    private List<String> players = new ArrayList<>();

    public DynmapParser(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public JSONObject getDynmapJSONData() throws IOException {
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(" " + sc.next());
        }
        JSONObject result = new JSONObject(sb.toString());

        return result;
    }

    public JSONArray getDynmapPlayers() throws IOException {
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(" " + sc.next());
        }
        JSONObject result = new JSONObject(sb.toString());

        JSONArray playerList = new JSONArray();

        for (Object player : result.getJSONArray("players")) {
            playerList.put(player);
        }

        return playerList;
    }

    public List<JSONObject> getDynmapPlayerObjects() throws IOException {
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(" " + sc.next());
        }
        JSONObject result = new JSONObject(sb.toString());

        List<JSONObject> playerList = new ArrayList<>();

        for (Object player : result.getJSONArray("players")) {
            playerList.add(new JSONObject(player.toString()));
        }

        return playerList;
    }

    public JSONArray getDynmapNations() throws IOException {
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(" " + sc.next());
        }
        JSONObject result = new JSONObject(sb.toString());


        return result.getJSONArray("updates");
    }

    public List<JSONObject> getDynmapNationLands(String nationName) throws IOException {
        List<JSONObject> nationsList = new ArrayList<>();

        for (Object nation : getDynmapNations()) {
            nationsList.add(new JSONObject(nation.toString()));
        }

        List<JSONObject> matchingNations = new ArrayList<>();

        for (JSONObject currentNation : nationsList) {


            if (currentNation.has("id") && currentNation.getString("id").startsWith(nationName)) {
                matchingNations.add(currentNation);
                break;
            }
        }

        return matchingNations;
    }

    // gets a nation by name
    public JSONObject getDynmapNation(String nationName) throws IOException {
        JSONArray nationsList = getDynmapNations();

        for (Object currentNation : nationsList) {
            JSONObject currentNationJSON = new JSONObject(currentNation.toString());


            if (currentNationJSON.has("label") && currentNationJSON.getString("label").equals(nationName)) {
                return currentNationJSON;
            }
        }

        return null;
    }

    // Gets the corner list of a nations borders
    public List<Point> getNationAreaCorners(JSONObject nation) throws IOException {
        Scanner sc = new Scanner(url.openStream());
        StringBuffer sb = new StringBuffer();
        while (sc.hasNext()) {
            sb.append(" " + sc.next());
        }
        JSONObject result = new JSONObject(sb.toString());

        List<Point> cornerList = new ArrayList<>();

        List<Integer> xCoords = new ArrayList<>();
        List<Integer> zCoords = new ArrayList<>();


        for (Object xCorner : nation.getJSONArray("x")) {
           xCoords.add(Integer.valueOf(xCorner.toString()));

        }
        for (Object zCorner : nation.getJSONArray("z")) {
            zCoords.add(Integer.valueOf(zCorner.toString()));
        }

        int i = 0;
        while (i < zCoords.size()) {

            cornerList.add(new Point(xCoords.get(i), zCoords.get(i)));

            i++;
        }

        return cornerList;
    }

    // Checks every territory in a faction for players
    public Set<String> getPlayersInAllNationLands(String nationName) throws IOException {
        List<JSONObject> nationLands = getDynmapNationLands(nationName);
        Set<String> playerList = new HashSet<>();

        for (JSONObject nation : nationLands) {


            playerList.addAll(getInsidePlayerNames(getNationAreaCorners(nation)));
        }

        return playerList;
    }


    // Checks if a players point is contained within the nations corner points
    public boolean isPointInPolygon(Point point, List<Point> polygonPoints) {
        Path2D.Double polygon = new Path2D.Double();
        Point start = polygonPoints.get(0);
        for (int i = 0; i < polygonPoints.size(); i++) {
            if (i == 0) {
                polygon.moveTo(start.x, start.y);
            }
            else {
                polygon.lineTo(polygonPoints.get(i).x, polygonPoints.get(i).y);
            }
        }
        polygon.closePath();
        Rectangle polySquare = polygon.getBounds();
        polySquare.setSize(polygon.getBounds().width * 2, polygon.getBounds().height * 2);
        return polySquare.contains(point.x, point.y);
    }
    public Set<String> getInsidePlayerNames(List<Point> area) throws IOException {
        JSONArray players = getDynmapPlayers();
        HashMap<String, Point> playerPointMap = new HashMap<>();
        Set<String> playerNames = new HashSet<>();

        for (Object player : players) {
            JSONObject playerObject = new JSONObject(player.toString());
            Point playerPoint = new Point(playerObject.getInt("x"), playerObject.getInt("z"));
            playerPointMap.put(playerObject.getString("name"), playerPoint);
        }

        playerPointMap.entrySet().forEach(entry -> { if(isPointInPolygon(entry.getValue(), area)) { playerNames.add(entry.getKey()); } else Logger.log(entry.getKey() + " failed check"); });

        playerNames.forEach(str -> Logger.log(ConsoleColors.BLUE_BACKGROUND + str + " detected inside!"));

        return playerNames;
    }

    public List<String> getNationPlayers(JSONObject nation) {
        String descriptionBox = nation.getString("desc");

        String unparsedMembers = descriptionBox.substring(descriptionBox.lastIndexOf("Members"))
                .replace("Members <span style=\"font-weight:bold\">", "")
                .replace("</span></div></div>", "");

        List<String> parsedMembers = new ArrayList<>(List.of(unparsedMembers.split(",")));

        parsedMembers.forEach(str -> Logger.log(ConsoleColors.BLUE_BACKGROUND + str));

        return parsedMembers;
    }

    public List<String> getAllNationPlayers(String nationName) throws IOException {
        Set<String> playerList = new HashSet<>();

        for (Object nation : getDynmapNationLands(nationName)) {
            JSONObject nationObject = new JSONObject(nation.toString());

            playerList.addAll(getNationPlayers(nationObject));
        }

        return playerList.stream().toList();
    }

    public List<String> getPlayerNames() throws IOException {
        List<JSONObject> players = getDynmapPlayerObjects();
        List<String> playerNames = new ArrayList<>();

        for (JSONObject player : players) {
            playerNames.add(player.getString("name"));
        }

        return playerNames;
    }

    public JSONObject getPlayerByName(String playerName) throws IOException {
        List<JSONObject> players = getDynmapPlayerObjects();
        JSONObject player = new JSONObject();

        for (JSONObject currentPlayer : players) {
            if (player.has("name") && player.getString("name").equals(playerName)) {
                player = currentPlayer;
            }
        }

        return player;
    }

    public void checkLogOffs() throws IOException {
        List<String> newPlayers = getPlayerNames();

        for (String player : players) {
            if (!newPlayers.contains(player)) {
                Main.getDiscordBot().checkLogOut(getPlayerByName(player));
            }
        }

        players = newPlayers;
    }

}
