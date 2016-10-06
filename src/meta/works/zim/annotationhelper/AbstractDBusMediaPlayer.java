package meta.works.zim.annotationhelper;

/**
 * Created by robert on 2016-10-06 11:30.
 */
public abstract
class AbstractDBusMediaPlayer
{
	abstract String getDBusSuffix();
	abstract void onBegin();
	abstract void onPaused();
	abstract void onResume();
	abstract void onStopOrFinished();
}
