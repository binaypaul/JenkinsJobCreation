package RJJI;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.json.JSONObject;

public class Mian {
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final String userName = "user";
	private final String token = "1147267b1545903025734a35cb1a87f949";
	private int sleepTimeInSec = 2*1000;
	private int retryCountLimit = 1800;

	public static void main(String[] args) throws IOException, InterruptedException {
		Mian obj = new Mian();
		String triggerJobURL = "http://localhost:8888/job/TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod/buildWithParameters"
				+ "?BRANCH_NAME=SOLVE-3676-trigger-jenkins-job-remotely"
				+ "&DELETE_EXISTING_POD=true"
				+ "&BUILDTAG=0.2.8832"
				+ "&PD_BUILD=0.1.1521"
				+ "&POSTGRES=false";
		HttpResponse<String> triggerJobURLResponse = obj.sendPost11(triggerJobURL);

				if(triggerJobURLResponse.statusCode() == 201) {
					//Get job id of created job.
					long jobIDOfStartJob = obj.getJobId11(triggerJobURLResponse);
					//Check job completion status.
					System.out.println("TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod Job completion status: " + obj.checkStartJobResultStatus11(jobIDOfStartJob));
				}
	}

	private long getJobId11(HttpResponse<String> startJobPostRes) throws IOException, InterruptedException {
		if(startJobPostRes.headers().firstValue("Location").isPresent()) {
			Optional<String> locationValueHeader =  startJobPostRes.headers().firstValue("Location");
			String startJobPostResLocationValue = locationValueHeader.orElseThrow();
			String getJobIDOfStartJobURL = startJobPostResLocationValue + "api/json";
			System.out.println("getJobIDOfStartJobURL: " + getJobIDOfStartJobURL);
			int retryCount = 0;
			Optional<Long> jobIDOfStartJobFromJobQueueOp = Optional.empty();
			while(retryCount < retryCountLimit) {
				retryCount++;
				Thread.sleep(sleepTimeInSec);
				JSONObject startJobStatusFromJobQueueJsonObject = sendGet11(getJobIDOfStartJobURL);
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

	private String checkStartJobResultStatus11(long jobIDOfStartJob) throws InterruptedException, IOException {String jobResultCheckURL = "http://localhost:8888/job/TriggerBranchJob_ConditionallyDeleteMyPod_CreateMyPod/" + jobIDOfStartJob + "/api/json";
	int retryCount = 0;
	String startJobResultStatus = "";
	while(retryCount < retryCountLimit) {
		retryCount++;
		Thread.sleep(sleepTimeInSec);
		JSONObject startJobStatusJsonObject = sendGet11(jobResultCheckURL);
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
	
	private JSONObject sendGet11(String url) throws IOException, InterruptedException {
		HttpRequest getHttpRequest = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", getAuthHeader())
				.GET()
				.build();
		HttpResponse<String> getHttpResponse = httpClient.send(getHttpRequest, BodyHandlers.ofString());
		
		if(getHttpResponse.statusCode() == 200) {
			System.out.println("GET successful with url: " + url);
			return new JSONObject(getHttpResponse.body());
		} else {
			throw new IOException("POST unsuccessful with url: " + url);
		}
	}

	private HttpResponse<String> sendPost11(String url) throws IOException, InterruptedException {
		//		HttpClient client = HttpClient.newHttpClient();
		HttpRequest postHttpRequest = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Authorization", getAuthHeader())
				.POST(BodyPublishers.noBody())
				.build();
		HttpResponse<String> postHttpResponse = httpClient.send(postHttpRequest, BodyHandlers.ofString());
		if(postHttpResponse.statusCode() == 201) {
			System.out.println("POST successful with url: " + url);
		} else {
			throw new IOException("POST unsuccessful with url: " + url);
		}
		return postHttpResponse;
	}

	private String getAuthHeader() {
		String credential = userName + ":" + token;
		return "Basic " + Base64.getEncoder().encodeToString(credential.getBytes());
	}
}
