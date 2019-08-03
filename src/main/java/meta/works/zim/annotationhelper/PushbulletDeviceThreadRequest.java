package meta.works.zim.annotationhelper;

import com.github.sheigutn.pushbullet.http.GetRequest;
import com.google.gson.JsonObject;

/**
 * Created by robert on 2018-12-24 13:04.
 */
public
class PushbulletDeviceThreadRequest extends GetRequest<JsonObject>
{
	public
	PushbulletDeviceThreadRequest(String deviceId)
	{
		super(String.format("/permanents/%s_threads", deviceId));
	}

	public
	PushbulletDeviceThreadRequest(String deviceId, String threadId)
	{
		super(String.format("/permanents/%s_thread_%s", deviceId, threadId));
	}
}
