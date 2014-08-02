package org.magnum.dataup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import retrofit.mime.TypedFile;

@Controller
public class VideoService {
	
	// Some constants
	public static final String DATA_PARAMETER = VideoSvcApi.DATA_PARAMETER; //"data";
	public static final String ID_PARAMETER = VideoSvcApi.ID_PARAMETER; //"id";
	public static final String VIDEO_SVC_PATH = VideoSvcApi.VIDEO_SVC_PATH; //"/video";
	public static final String VIDEO_DATA_PATH = VideoSvcApi.VIDEO_DATA_PATH; 
						//VIDEO_SVC_PATH + "/" + ID_PARAMETER + "/" + DATA_PARAMETER;

	// An in-memory list that the servlet uses to store the
	// videos that are sent to it by clients
	//private List<Video> videos = new CopyOnWriteArrayList<Video>();
	
	// Video unique ID
	private static final AtomicLong currentId = new AtomicLong(0L);
	
	// An in-memory data structure to store videos indexed by their 
	// IDs
	private Map<Long, Video> videos = new HashMap<Long, Video>();

	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation. value param must be from VideoSvcApi
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody List<Video> //Map<Long, Video> 
	getVideoList(){
		return new ArrayList<Video>(videos.values());
	}
	
	
	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The Video object
	// contains info about the actual video data. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to convert the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.

	// For some ways to improve the validation of the data
	// in the Video object, please see this Spring guide:
	// http://docs.spring.io/spring/docs/3.2.x/spring-framework-reference/html/validation.html#validation-beanvalidation
	//
	@RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
	public 
	@ResponseBody Video 
	addVideoInfo(@RequestBody Video v) {	
		Video newVideo = save(v);
		String newVideoUrl = getDataUrl(newVideo.getId());
		newVideo.setDataUrl(newVideoUrl);
		return newVideo;
	}
	
	
	// Receives POST request to /video/{id}/data storing the mpeg/MP4 video data 
	// for previously added Video objects by sending multipart POST requests to the server.
	// The URL that the POST requests should send includes the ID of the
	// Video object that this data should be associated with (e.g., replace {id} in
	// the url /video/{id}/data with a valid ID of a video, such as /video/1/data
	// -- assuming that "1" is a valid ID of a video). 
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
	public 
	@ResponseBody VideoStatus 
	addVideoData(@PathVariable(ID_PARAMETER) String id, 
				 @RequestParam(DATA_PARAMETER) TypedFile videoData) {
		
		return new VideoStatus(VideoStatus.VideoState.READY);
	}
	

	// This endpoint should return the video data that has been associated with
	// a Video object or a 404 if no video data has been set yet. The URL scheme
	// is the same as in the method above and assumes that the client knows the ID
	// of the Video object that it would like to retrieve video data for.
	  
/*	  This method uses Retrofit's @Streaming annotation to indicate that the
	  method is going to access a large stream of data (e.g., the mpeg video 
	  data on the server). The client can access this stream of data by obtaining
	  an InputStream from the Response as shown below:
	  
	  VideoSvcApi client = ... // use retrofit to create the client
	  Response response = client.getData(someVideoId);
	  InputStream videoDataStream = response.getBody().in();
*/	 
	@RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
    void getVideoData(@PathVariable(ID_PARAMETER) long id,
    				  HttpServletResponse response) {
		// TODO: find appropriate video using id
		// TODO: return video in multipart response body
	}
	
	
	/********* Private Parts **********/
	
    private String getDataUrl(long videoId){
        //String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
    	String url = getUrlBaseForLocalServer() + 
    					VIDEO_SVC_PATH + "/" + videoId + "/" + DATA_PARAMETER;
        return url;
    }

 	private String getUrlBaseForLocalServer() {
 		// Note: RequestContextHolder holds HTTP Request data. Spring invoked this
 		// thread to handle this HTTP Request and prepared the data.
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = "http://" + request.getServerName() + 
			   ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
	   return base;
	}
	
	// Generate unique ID for the new video and store in local
	// data structure.
 	private Video save(Video entity) {
		checkAndSetId(entity);
		videos.put(entity.getId(), entity);
		return entity;
	}
 	// Test if video info already has ID. If not, initialize it w/ new one.
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
}
