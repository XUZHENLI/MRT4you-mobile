package com.example.mrt4you_mobile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SearchRouteActivity extends BaseActivity implements RouteFragment.iRouteFragment{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        replaceRouteFragment(null);

        //retrieve list of stations to populate auto-suggestions for search bars
        AutoCompleteTextView startingStation = findViewById(R.id.starting_station);
        AutoCompleteTextView destination = findViewById(R.id.destination);
        InputStream input = getResources().openRawResource(R.raw.stations);
        String content;
        try {
            content = getStationsFromFile(input);
            if (content!=null) {
                String[] stations = content.split(";");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        R.layout.support_simple_spinner_dropdown_item, stations);
                startingStation.setAdapter(adapter);
                destination.setAdapter(adapter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //set onclicklistener to search button to call shortest path algorithm and
        //replace fragment with result
        ImageButton searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v -> {
            hideSoftKeyboard(this);

            String startingStationName = startingStation.getText().toString().trim();
            String destinationName = destination.getText().toString().trim();

            if (startingStationName.isEmpty() || destinationName.isEmpty())
            {
                Toast.makeText(this, "Please input starting station and " +
                        "destination", Toast.LENGTH_SHORT).show();
            }
            else
            {
                new Thread(() ->
                {
                    Graph graph = Graph.createMRTGraph();
                    try
                    {
                        graph.updateGraphFromWebAPI();
                    }
                    catch (JSONException | IOException e)
                    {
                        e.printStackTrace();
                    }
                    Route result = Dijkstra
                            .shortestPathAndDistanceFromSourceToDestination
                                    (startingStationName, destinationName, graph);
                    if (result != null)
                    {
                        runOnUiThread(() -> {
                            replaceRouteFragment(result);
                            // the prints below are just to check if data is being passed properly
                            System.out.println(result.getPath());
                            System.out.println(result.getTotalTime());
                            System.out.println(result.getInterchanges().size());
                            for (Subroute sr : result.getSubroutes())
                            {
                                System.out.println(sr.getLine());
                                System.out.println("Towards " + sr.getDirection());
                                System.out.println(sr.getNoOfStations());
                                System.out.print(sr.getNoOfMins() + "\n");
                            }
                        });
                    }
                    else
                    {
                        runOnUiThread(() -> {
                            replaceRouteFragment(null);
                            Toast.makeText(this, "There is no path available!",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }
        });
    }

    private String getStationsFromFile(InputStream input) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        while ((line = br.readLine())!=null) {
            content.append(line).append(";");
        }
        return content.toString();
    }

    public void replaceRouteFragment(Route route) {
        RouteFragment fragment = new RouteFragment();
        if (route!=null) {
            Bundle arguments = new Bundle();
            arguments.putSerializable("route", route);
            fragment.setArguments(arguments);
        }
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction trans = fm.beginTransaction();
        trans.replace(R.id.fragment_route_container, fragment);
        if(route!=null) {
            trans.addToBackStack(null);
        }
        trans.commit();
    }


    @Override
    int getContentViewId() {
        return R.layout.activity_search_route;
    }

    @Override
    int getNavigationMenuItemId() {
        return R.id.action_search;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void bookmarkClicked() {}

/*    @Override
    public void onBackPressed()
    {
        //if back stack only consist of the first fragment insertion, exit app
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if(backStackCount == 1) {
            System.exit(1);
        } else {
            super.onBackPressed();
        }
    }*/
}