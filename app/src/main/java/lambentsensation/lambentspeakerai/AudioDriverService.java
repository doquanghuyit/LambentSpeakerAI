package lambentsensation.lambentspeakerai;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by doquanghuy on 12/5/17.
 */

public class AudioDriverService extends Service {



    @Override
    public void onCreate() {
        super.onCreate();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }


}
