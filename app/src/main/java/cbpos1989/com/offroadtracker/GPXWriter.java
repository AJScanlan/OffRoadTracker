package cbpos1989.com.offroadtracker;

import java.io.File;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;



/**
 * Created by carlosefonseca.
 */

public class GPXWriter {
    private static final String TAG = GPXWriter.class.getName();
    private static MapsActivity mapsActivity;
    public GPXWriter(MapsActivity mapsActivity){
        this.mapsActivity = mapsActivity;
    }

    public static void writePath(File file, String n, List<Location> points) {

        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        String name = "<name>" + n + "</name><trkseg>\n";

        String segments = "";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        for (Location l : points) {
            segments += "<trkpt lat=\"" + l.getLatitude() + "\" lon=\"" + l.getLongitude() + "\"> <time>" + df.format(new Date(l.getTime())) + "</time></trkpt>\n";
        }

        String footer = "</trkseg></trk></gpx>";

        try {
            FileWriter writer = new FileWriter(file, true);
            writer.append(header);
            writer.append(name);
            writer.append(segments);
            writer.append(footer);
            writer.flush();
            writer.close();

            Log.i(TAG, "Saved " + points.size() + " points.");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(mapsActivity.getApplicationContext(),"File not found",Toast.LENGTH_SHORT);
            Log.e(TAG, "Error Writting Path",e);
        }
    }
}
