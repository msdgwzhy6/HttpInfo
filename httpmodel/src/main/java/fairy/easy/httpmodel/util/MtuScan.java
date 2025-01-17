package fairy.easy.httpmodel.util;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MtuScan {
    private int size;

    private String host;

    private volatile boolean cancelMtuScan;

    public MtuScan(String host) {
        this.host = host;
        this.cancelMtuScan = false;
    }

    public void cancelMtuScan() {
        this.cancelMtuScan = true;
    }

    public List<Integer> start() {
        List<Integer> mtuList = new ArrayList<>();
        int[] arrayOfInt = {1500, 1492, 1472, 1468, 1430, 1400, 576};
        for (int i : arrayOfInt) {
            if (cancelMtuScan) {
                break;
            }
            this.size = i;
            if (checkMtuFromPing()) {
                mtuList.add(size);
            } else {
                do {
                    if (cancelMtuScan) {
                        break;
                    }
                    this.size = size - 28;
                    if (checkMtuFromPing()) {
                        mtuList.add(size);
                        break;
                    }
                } while (this.size > 100);
            }
        }
        return mtuList;
    }

    private boolean checkMtuFromPing() {
        String param = ping(createSimplePingCommand());
        if (TextUtils.isEmpty(param)) {
            return false;
        }
        if (param.toLowerCase().contains("icmp_seq")) {
            if (param.toLowerCase().contains("df")) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private String createSimplePingCommand() {
        Object[] arrayOfObject = new Object[3];
        arrayOfObject[0] = this.size;
        arrayOfObject[1] = this.host;
        return String.format("/system/bin/ping -M do -c 1 -s %d %s", arrayOfObject);
    }

    private String ping(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while (null != (line = reader.readLine())) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return null;
    }
}
