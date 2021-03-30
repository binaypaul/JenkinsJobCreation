//dependencies:
//'org.apache.httpcomponents:httpclient:4.5.13'
//'org.json:json:20210307'
package RJJI;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class Mian {
	private final CloseableHttpClient httpClient = HttpClients.createDefault();
	private final String userName = "user";
	private final String token = "1147267b1545903025734a35cb1a87f949";
	private int sleepTimeInSec = 2*1000;
	private int retryCountLimit = 1800;

	public static void main(String[] args) throws IOException, InterruptedException, HttpException {
		Mian obj = new Mian();
		String triggerJobURL = "http://localhost:8888/job/TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod/buildWithParameters"
				+ "?BRANCH_NAME=SOLVE-3676-trigger-jenkins-job-remotely"
				+ "&DELETE_EXISTING_POD=true"
				+ "&BUILDTAG=0.2.8832"
				+ "&PD_BUILD=0.1.1521"
				+ "&POSTGRES=false";
		HttpResponse triggerJobURLResponse = obj.sendPost(triggerJobURL);
		if(triggerJobURLResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
			//Get job id of created job.
			long jobIDOfStartJob = obj.getJobId(triggerJobURLResponse);
			//Check job completion status.
			System.out.println("TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod Job completion status: " + obj.checkStartJobResultStatus(jobIDOfStartJob));
			
		}
		obj.close();
	}

	private long getJobId(HttpResponse startJobPostRes) throws IOException, InterruptedException, HttpException {
			if(startJobPostRes.containsHeader("Location")) {
				Optional<Header> locationValueHeader =  Arrays.asList(startJobPostRes.getAllHeaders()).stream().filter(h -> h.getName().equals("Location")).findFirst();
				String startJobPostResLocationValue = locationValueHeader.orElseThrow().getValue();
				String getJobIDOfStartJobURL = startJobPostResLocationValue + "api/json";
				System.out.println("getJobIDOfStartJobURL: " + getJobIDOfStartJobURL);
				int retryCount = 0;
				Optional<Long> jobIDOfStartJobFromJobQueueOp = Optional.empty();
				while(retryCount < retryCountLimit) {
					retryCount++;
					Thread.sleep(sleepTimeInSec);
					JSONObject startJobStatusFromJobQueueJsonObject = sendGet(getJobIDOfStartJobURL);
					if(startJobStatusFromJobQueueJsonObject.has("executable")) {
						jobIDOfStartJobFromJobQueueOp = Optional.ofNullable(startJobStatusFromJobQueueJsonObject.getJSONObject("executable").getLong("number"));
						break;
					}
				}
				Long jobIDOfStartJobFromJobQueue = jobIDOfStartJobFromJobQueueOp.orElseThrow();
				System.out.println("jobIDOfStartJobFromJobQueue: " + jobIDOfStartJobFromJobQueue);
				System.out.println("retryCount: " + retryCount);
				System.out.println();
				return jobIDOfStartJobFromJobQueue;
			} else {
				throw new NoSuchElementException("Location not found in startJobPostRes: " + startJobPostRes);
			}
	}
	
	private String checkStartJobResultStatus(long jobIDOfStartJob) throws IOException, InterruptedException, HttpException {
		//Check job completion status.
		String jobResultCheckURL = "http://localhost:8888/job/TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod/" + jobIDOfStartJob + "/api/json";
		int retryCount = 0;
		String startJobResultStatus = "";
		while(retryCount < retryCountLimit) {
			retryCount++;
			Thread.sleep(sleepTimeInSec);
			JSONObject startJobStatusJsonObject = sendGet(jobResultCheckURL);
			if(startJobStatusJsonObject.has("result") && !startJobStatusJsonObject.isNull("result")) {
				startJobResultStatus = startJobStatusJsonObject.getString("result");
				break;
			}
		}
		System.out.println("startJobResultStatus: " + startJobResultStatus);
		System.out.println("retryCount: " + retryCount);
		System.out.println();
		return startJobResultStatus;
	}

	private JSONObject sendGet(String url) throws IOException, HttpException{
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpResponse response = httpClient.execute(getRequest);
			System.out.println("GET request status code: " + response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				System.out.println("GET successful with url: " + url);
				HttpEntity responseHttpEntity = response.getEntity();
				if (responseHttpEntity != null) {
					JSONObject responseJsonObject = new JSONObject(EntityUtils.toString(responseHttpEntity));
					return responseJsonObject;
				}
			} else {
				throw new HttpException("GET unsuccessful with url: " + url);
			}
			return new JSONObject();
		}		
	}

	private HttpResponse sendPost(String url) throws IOException, HttpException {
		HttpPost postRequest = new HttpPost(url);
		postRequest.addHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpResponse response = httpClient.execute(postRequest);
			System.out.println("POST request status code: " + response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				System.out.println("POST successful with url: " + url);
			} else {
				throw new HttpException("POST unsuccessful with url: " + url);
			}
			return response;
		}
	}
	private void close() throws IOException {
		httpClient.close();
	}

	private String getAuthHeader() {
		String credential = userName + ":" + token;
		String auth = new String(Base64.encodeBase64(credential.getBytes()));
		return "Basic " + auth;
	}
}
