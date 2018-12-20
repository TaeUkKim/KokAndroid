package neolabs.kok.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.CalloutBalloonAdapter;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;
import java.util.List;

import neolabs.kok.sutff.GPSInfo;
import neolabs.kok.R;
import neolabs.kok.data.KokData;
import neolabs.kok.item.KokItem;
import neolabs.kok.retrofit.RetrofitExService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, MapView.MapViewEventListener, MapView.POIItemEventListener {

    FloatingActionButton gotoprofile;
    FloatingActionButton addkok;
    FloatingActionButton goChat;

    List<KokItem> items = new ArrayList<>();

    String[] userauthidarray = new String[99999];
    String[] usernamearray = new String[99999];
    String[] kokidarray = new String[99999];
    String[] kokcomment = new String[99999];

    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;

    private GPSInfo gps;
    MapView mapView;
    ViewGroup mapViewContainer;
    MapPoint mapPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getGpsData();

        gotoprofile = findViewById(R.id.myprofile);
        addkok = findViewById(R.id.addkok);
        goChat = findViewById(R.id.chating);

        gotoprofile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        addkok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddKokActivity.class);
                startActivity(intent);
            }
        });

        goChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ChatlistActivity.class);
                startActivity(intent);
            }
        });
    }

    //가까이에 있는 콕을 서버에서 부터 받아온다.
    public void getkokfromserver (String latitude, String longitude) {
        Retrofit client = new Retrofit.Builder().baseUrl(RetrofitExService.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        RetrofitExService service = client.create(RetrofitExService.class);
        Call<List<KokData>> call = service.getPick(latitude, longitude);
        call.enqueue(new Callback<List<KokData>>() {
            @Override
            public void onResponse(@NonNull Call<List<KokData>> call, @NonNull retrofit2.Response<List<KokData>> response) {
                Log.d("softtag", "isitworkingcheck");
                switch (response.code()) {
                    case 200:
                        //일단 콕들을 전부 다 지우고 시작한다.
                        mapView.removeAllPOIItems();
                        for(int i = 0; i < response.body().size(); i++) {
                            userauthidarray[i] = response.body().get(i).getUserauthid();
                            kokidarray[i] = response.body().get(i).getId();
                            kokcomment[i] = response.body().get(i).getMessage();
                            usernamearray[i] = response.body().get(i).getUsernickname();

                            MapPOIItem marker = new MapPOIItem();
                            List<Double> point = response.body().get(i).getLocation().getCoordinates();
                            marker.setItemName(response.body().get(i).getUsernickname() + "의 Kok!");
                            marker.setTag(i);
                            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(point.get(1), point.get(0)));

                            marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

                            marker.setCustomImageResourceId(R.drawable.custom_marker_red);
                            marker.setCustomImageAutoscale(false);
                            marker.setCustomImageAnchor(0.5f, 1.0f);
                            mapView.addPOIItem(marker);

                            Log.d("tag", String.format("%f", point.get(0)));
                            point.clear();
                        }
                        //출처: http://jekalmin.tistory.com/entry/Gson을-이용한-json을-객체에-담기 [jekalmin의 블로그]
                        break;
                    case 409:
                        Toast.makeText(MainActivity.this, "에러가 발생하였습니다.", Toast.LENGTH_SHORT).show();
                    default:
                        Log.e("asdf", response.code() + "");
                        break;
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<KokData>> call, @NonNull Throwable t) {
                Log.d("checkonthe", "error");
            }
        });
    }

    //위도와 경도를 얻어온다.
    public void getGpsData() {
        if(!isPermission){
            callPermission();
        }

        gps = new GPSInfo(MainActivity.this);

        // GPS 사용유무 가져오기
        if (gps.isGetLocation()) {
            //GPSInfo를 통해 알아낸 위도값과 경도값
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            Log.d("latitude", String.format("%f", latitude));
            Log.d("longitude", String.format("%f", longitude));

            //중복 선언로 인한 FC방지를 위한 if문
            if(mapView == null) {
                mapView = new MapView(this);
                mapView.setDaumMapApiKey("beb4ae99eb57de8785135bb2c5484f33");
                mapViewContainer = (ViewGroup) findViewById(R.id.mapView);
                mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude);
                mapView.setMapCenterPoint(mapPoint, true);
                mapViewContainer.addView(mapView);
                mapView.setPOIItemEventListener(this);
                mapView.setCalloutBalloonAdapter(new CustomCalloutBalloonAdapter());
            }

            getkokfromserver(String.format("%f", latitude), String.format("%f", longitude));
        } else {
            // GPS 를 사용할수 없으므로
            gps.showSettingsAlert();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getGpsData();
    }

    @Override
    public void onRefresh() {

    }

    class CustomCalloutBalloonAdapter implements CalloutBalloonAdapter {
        private final View mCalloutBalloon;

        public CustomCalloutBalloonAdapter() {
            mCalloutBalloon = getLayoutInflater().inflate(R.layout.customtag, null);
        }

        @Override
        public View getCalloutBalloon(MapPOIItem poiItem) {
            ((ImageView) mCalloutBalloon.findViewById(R.id.badge)).setImageResource(R.mipmap.ic_launcher);
            ((TextView) mCalloutBalloon.findViewById(R.id.title3)).setText(poiItem.getItemName());
            ((TextView) mCalloutBalloon.findViewById(R.id.desc)).setText(kokcomment[poiItem.getTag()]);
            return mCalloutBalloon;
        }

        @Override
        public View getPressedCalloutBalloon(MapPOIItem poiItem) {
            return null;
        }
    }

    //맵뷰가 처음 실행되었을때 호출되는 메서드.
    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    //맵뷰에서 생겨난 POI를 선택했을때 호출되는 함수.
    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {

    }

    //맵뷰에서 생겨난 POI를 선택하고 나온 풍선을 선택했을때 호출되는 함수.
    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        Log.d("selected!", "tag");
        Intent intent = new Intent(MainActivity.this, KokCommentActivity.class);
        intent.putExtra("username", usernamearray[mapPOIItem.getTag()]);
        intent.putExtra("kokcomment", kokcomment[mapPOIItem.getTag()]);
        intent.putExtra("userauthid", kokidarray[mapPOIItem.getTag()]); //유저가 아니라 콕 고유 authid이다.... 착각 ㄴㄴ
        startActivity(intent);
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAccessFineLocation = true;
        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    // 전화번호 권한 요청
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }
}
