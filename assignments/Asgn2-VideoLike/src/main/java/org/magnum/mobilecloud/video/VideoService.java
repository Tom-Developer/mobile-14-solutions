/*
 * 
 * Copyright 2014 Tom Parpura
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.mobilecloud.video;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import retrofit.http.Path;

import com.google.common.collect.Lists;


@Controller
public class VideoService {
	
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;
	
		
	// POST /oauth/token It's implemented by OAuth2SecurityConfiguration
	// Receives POST requests to /oauth/token - the access point for the
	// OAuth 2.0 Password Grant flow. Clients should be able to submit a request
	// with their username, password, client ID, and client secret, encoded
	// as described in the OAuth lecture videos.
	
	
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation. value param must be from VideoSvcApi
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> 
	getVideoList(){
		return Lists.newArrayList(videos.findAll());
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
	
	//@PreAuthorize( "hasRole(user0)" )
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video
	addVideoInfo(@RequestBody Video v){
		 Video savedVideo = videos.save(v);
		 return savedVideo;
	}
	
	
	// Receives GET requests to /video/{id} and returns video w/ given ID or 404.
	// This endpoint should return the video data that has been associated with
	// a Video object or a 404 if no video data has been set yet. The URL scheme
	// is the same as in the method above and assumes that the client knows the ID
	// of the Video object that it would like to retrieve video data for.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method=RequestMethod.GET)
    @ResponseBody Video
    getVideoData(@PathVariable("id") long id,
    			 HttpServletResponse response) throws IOException {
		
		//Video v = videos.get(Long.parseLong(id));
		Video v = videos.findOne(id);
		if( v == null ) {
			//String errorMsg = String.format("Video with %s hasn't been found on the server", id);
		    //log.error( errorMsg );
			//response.sendError(HttpServletResponse.SC_NOT_FOUND, errorMsg );
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			//response.flushBuffer();
		}
		return v;
	}
	
	// Receives POST requests to /video/{id}/like and returns 200 Ok on success,
	// 404 if the video is not found, or 400 if the user has already liked the video.
	// The service should  keep track of which users have liked a video
	// and prevent a user from liking a video twice.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method=RequestMethod.POST)
	public void 
	addVideoLike(@PathVariable("id") long id,
				 Principal p, 
				 HttpServletResponse response) throws IOException {
		

		Video v = videos.findOne(id);	
		if( v == null ) {
			// video not found
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
	    }
		//One can also get current user name with:
		//User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	    //String name = user.getUsername(); //see http://goo.gl/0l46v0
		//String userName = p.getName(); 
		boolean rc = v.getLikesUserNames().contains( p.getName() );
		if( rc ) { 
			// current user already liked this video
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
			
		} else {
			// set video like
			v.setLikes( v.getLikes() + 1L );
			v.addLikesUserName( p.getName() );
			videos.save(v);
			response.setStatus(HttpServletResponse.SC_OK); 
		}
	}

	
	// Receives POST requests to /video/{id}/unlike and returns 200 Ok on success,
	// 404 if the video is not found, or 400 if the user has not previously liked
	// the specified video.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method=RequestMethod.POST)
	public void 
	subtractVideoLike(@PathVariable("id") long id,
					  Principal p,
					  HttpServletResponse response) throws IOException {
		
		Video v = videos.findOne(id);	
		if( v == null ) {
			// video not found
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
	    }
		
		boolean rc = v.getLikesUserNames().contains( p.getName() );
		if( rc ) {
			// remove video like
			v.setLikes( v.getLikes() - 1 );
			v.subtractLikesUserName( p.getName() );
			videos.save(v);
			response.setStatus(HttpServletResponse.SC_OK);

		} else {
			// current user hasn't liked this video
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}

	
	// Receives GET requests to /video/{id}/likedby and returns a list of 
	// the string usernames of the users that have liked the specified video. 
	// If the video is not found, a 404 error should be generated.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> 
	getVideoLikesUsers(@PathVariable("id") long id,
		    		   HttpServletResponse response) throws IOException {
		
		Video v = videos.findOne(id);
		if( v == null ) {
			// video not found
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return new ArrayList<String>(); //null;
	    }
		
		response.setStatus(HttpServletResponse.SC_OK);
		return v.getLikesUserNames();
	}
	
	
	// Receives GET requests to /video/search/findByTitle and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> 
	findByTitle(
			// Tell Spring to use the "title" parameter in the HTTP request's query
			// string as the value for the title method parameter
			@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title ){
		return videos.findByName(title);
	}
	
	
	// Receives GET requests to /video/search/findByDurationLessThan?duration={duration}
	// and returns all Videos that have duration less than specified duration
	// parameter value that is passed by the client. It returns empty list if no
	// videos were found.
	@RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> 
	findByDurationLessThan(
			// Tell Spring to use the "duration" parameter in the HTTP request's query
			// string as the value for the duration method parameter
			@RequestParam(VideoSvcApi.DURATION_PARAMETER) long duration ){
		return videos.findByDurationLessThan(duration);
	}
	
}
