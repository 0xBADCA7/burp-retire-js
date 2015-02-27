package com.h3xstream.retirejs.repo;

import com.esotericsoftware.minlog.Log;
import com.h3xstream.retirejs.util.RegexUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;

public class VulnerabilitiesRepositoryLoader {

    /**
     * This switch will be need for the test case.
     */
    public static boolean syncWithOnlineRepository = true;


    public VulnerabilitiesRepository load() throws IOException {
        InputStream inputStream = null;

        if (syncWithOnlineRepository) { //Remote repository
            try {
                URL remoteRepo = new URL("https://raw.githubusercontent.com/Retirejs/retire.js/master/repository/jsrepository.json");
                URLConnection conn = remoteRepo.openConnection();
                conn.connect();
                inputStream = conn.getInputStream();

                Log.info("Loading the latest Retire.js repository");
                return loadFromInputStream(inputStream);
            } catch (UnknownHostException exception) {
                Log.error("Exception while loading the repository (Most likely unable to access the internet) " +
                        exception.getClass().getName() + ": " + exception.getMessage());
            } catch (IOException exception) { //If an problem occurs with the online file, the local repository is used.
                Log.error("Exception while loading the repository (Connection problem while loading latest repository from GitHub) " +
                        exception.getClass().getName() + ": " + exception.getMessage());
            } catch (RuntimeException exception) {
                Log.error("Exception while loading the repository (Unable to access GitHub ?) " +
                        exception.getClass().getName() + ": " + exception.getMessage());
            }
        }

        //Local version of the repository
        Log.info("Loading the local Retire.js repository (old cache version)");
        inputStream = getClass().getResourceAsStream("/retirejs_repository.json");
        return loadFromInputStream(inputStream);
    }

    public VulnerabilitiesRepository loadFromInputStream(InputStream in) throws IOException {
        JSONObject rootJson = new JSONObject(convertStreamToString(in));


        VulnerabilitiesRepository repo = new VulnerabilitiesRepository();

        int nbLoaded = 0;
        Iterator it = rootJson.keySet().iterator(); //Iterate on each library jquery, YUI, prototypejs, ...
        while (it.hasNext()) {
            String key = (String) it.next();
            JSONObject libJson = rootJson.getJSONObject(key);

            JsLibrary lib = new JsLibrary();

            if (libJson.has("vulnerabilities")) {
                JSONArray vulnerabilities = libJson.getJSONArray("vulnerabilities");

                lib.setName(key);
                //Log.debug("Building the library " + key);

                for (int i = 0; i < vulnerabilities.length(); i++) { //Build Vulnerabilities list
                    JSONObject vuln = vulnerabilities.getJSONObject(i);
                    String atOrAbove = vuln.has("atOrAbove") ? vuln.getString("atOrAbove") : null; //Optional field
                    String below = vuln.getString("below");
                    List<String> info = objToStringList(vuln.get("info"), false);
                    lib.getVulnerabilities().add(new JsVulnerability(atOrAbove, below, info));
                }
            }
            if (libJson.has("extractors")) {
                JSONObject extractor = libJson.getJSONObject("extractors");
                //Imports various lists
                if (extractor.has("func"))
                    lib.setFunctions(objToStringList(extractor.get("func"), false));
                if (extractor.has("filename"))
                    lib.setFilename(objToStringList(extractor.get("filename"), true));
                if (extractor.has("filecontent"))
                    lib.setFileContents(objToStringList(extractor.get("filecontent"), true));
                if (extractor.has("hashes"))
                    lib.setHashes(objToStringMap(extractor.get("hashes")));
                if (extractor.has("uri"))
                    lib.setUris(objToStringList(extractor.get("uri"), true));
            }
            //Once all the information have been collected, the library is ready to be cache.

            repo.addLibrary(lib);
            nbLoaded++;
            //System.out.println(libJson.toString());
        }
        Log.debug(nbLoaded + " loaded library.");
        return repo;
    }

    ///Convertion utility methods

    public List<String> objToStringList(Object obj, boolean replaceVersionWildcard) {
        JSONArray array = (JSONArray) obj;
        List<String> strArray = new ArrayList<String>(array.length());
        for (int i = 0; i < array.length(); i++) { //Build Vulnerabilities list

            if (replaceVersionWildcard) {
                strArray.add(RegexUtil.replaceVersion(array.getString(i)));
            } else {
                strArray.add(array.getString(i));
            }
        }
        return strArray;
    }

    public Map<String, String> objToStringMap(Object obj) {
        Map<String, String> finalMap = new HashMap<String, String>();

        JSONObject jsonObj = (JSONObject) obj;
        Iterator it = jsonObj.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();

            finalMap.put(key, jsonObj.getString(key));
        }
        return finalMap;
    }

    static String convertStreamToString(InputStream is) {
        try {
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
