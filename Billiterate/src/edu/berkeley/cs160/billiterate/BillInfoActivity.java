package edu.berkeley.cs160.billiterate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class BillInfoActivity extends Activity {
	
	// information for this screen
	String bill_title;
	String bill_summary;
	String representative = "";
	int billId;
	
	
	// get view widgets for modification
	LinearLayout bill_view;
	TextView bill_title_textview;
	ProgressBar	ratings;
	//ProgressBar down_ratings;
	//ProgressBar up_ratings;
	int likes;
	int dislikes;
	ImageButton like;
	ImageButton dislike;
	TextView summary;
	EditText commentBox;
	
	ShapeDrawable pgDrawable;
	
	boolean liked = false;;
	boolean disliked = false;

	List<Map<String, String>> data = new ArrayList<Map<String, String>>();
	SimpleAdapter adapter; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bill_info);
		
		Bundle extras = this.getIntent().getExtras();
		bill_title = extras.getString("title");
		bill_summary = extras.getString("summary");
		representative = extras.getString("representative");
		billId = extras.getInt("id");
		
		bill_view = (LinearLayout)findViewById(R.id.bill_view);
		bill_title_textview = (TextView)findViewById(R.id.title);
		ratings = (ProgressBar)findViewById(R.id.ratings);
		like = (ImageButton)findViewById(R.id.like);
		dislike = (ImageButton)findViewById(R.id.dislike);
		summary = (TextView)findViewById(R.id.bill_summary);
		commentBox = (EditText)findViewById(R.id.comment);
		
		// display bill information
		bill_title_textview.setText(bill_title);
		summary.setText(bill_summary);
		
		pgDrawable = new ShapeDrawable();
		pgDrawable.getPaint().setColor(Color.CYAN);
		ClipDrawable pgBar = new ClipDrawable(pgDrawable, Gravity.LEFT, ClipDrawable.HORIZONTAL);
		ratings.setProgressDrawable(pgBar);
		loadProgressBars();
		
		adapter = new SimpleAdapter(this, data, R.layout.comment_layout, 
					new String[] {"Name", "Comment", "ID"}, 
					new int[] {R.id.nameText, R.id.commentText, R.id.IDText});
		ListView comments = (ListView) findViewById(R.id.comments);
		comments.setAdapter(adapter);
		
		load(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bill_info, menu);
		return true;
	}
	
	public void likeBill(View v) {
		// update ratings and increment progress bar
		if (liked) {
			like.setBackgroundResource(R.drawable.thumbs_up_blk);
			likes = likes - 1;
			liked = false;
		} else {
			like.setBackgroundResource(R.drawable.thumbs_up_grn);
			dislike.setBackgroundResource(R.drawable.thumbs_down_blk);
			likes = likes + 1;
			liked = true;
			if (disliked) {
				dislikes = dislikes - 1;
				disliked = false;
			}
		}
		setProgressBars();
	}
	
	public void dislikeBill(View v) {
		// update ratings and decrement progress bar
		if (disliked) {
			dislike.setBackgroundResource(R.drawable.thumbs_down_blk);
			dislikes = dislikes - 1;
			disliked = false;
		} else {
			dislike.setBackgroundResource(R.drawable.thumbs_down_red);
			like.setBackgroundResource(R.drawable.thumbs_up_blk);
			dislikes = dislikes + 1;
			disliked = true;
			if (liked) {
				likes = likes - 1;
				liked = false;
			}
		}
		setProgressBars();
	}
	
	public void getInfo(View v) {
		String billQuery = bill_title.replace(" ", "+");
		String search = "http://www.google.com/search?q=" + billQuery;
		Uri uri = Uri.parse(search);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	public void load(View v) {
		LoadTask task = new LoadTask();
		task.execute();
	}
	
	public void contact(View v) {
		// takes user to representative's info screen
		Intent i = new Intent(this, RepresentativeActivity.class);
		i.putExtra("representative", representative);
		startActivity(i);
	}
	
	public void setProgressBars() {
		PostLikesTask task = new PostLikesTask();
		task.execute();
		System.err.println("Finished setting progress bars");
	}
	
	public void loadProgressBars() {
		LoadLikesTask task = new LoadLikesTask();
		task.execute();
		System.err.println("Finished loading progress bars");
	}
	
	private class PostLikesTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String...params) {
			String url = "http://billiterate.pythonanywhere.com/likes/" + Integer.toString(billId);
			HttpResponse response;
			HttpClient client = new DefaultHttpClient();
			try {
				HttpPost post = new HttpPost(url);
				List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
				postParameters.add(new BasicNameValuePair("likes", Integer.toString(likes)));
				postParameters.add(new BasicNameValuePair("dislikes", Integer.toString(dislikes)));
				postParameters.add(new BasicNameValuePair("trending", Integer.toString(likes + dislikes)));
				postParameters.add(new BasicNameValuePair("hash", Integer.toString(billId)));
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
				post.setEntity(entity);
				System.err.println("Posting " + postParameters.toString() + "to " + url);
				response = client.execute(post);
			} catch (ClientProtocolException cpe) {
				System.err.println("*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^");
				cpe.printStackTrace();
			} catch (IOException e) {
				System.err.println("*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^");
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String arg0) {
			//loadProgressBars();
			int total = likes + dislikes;
			System.err.println("The total is: " + total);
			if (total > 0) {
				int up = (likes * 100) / total;
				ratings.setBackgroundColor(Color.RED);
				//ratings.getProgressDrawable().setColorFilter(Color.GREEN, Mode.MULTIPLY);
				pgDrawable.getPaint().setColor(Color.GREEN);
				ratings.setProgress(up);
				System.err.println("=============  Set ProgressBar Task  =============");
				System.err.println("Likes: " + likes + " Dislikes: " + dislikes);
				System.err.println("==================================================");
			} else {
				System.err.println("The ratings bar should be reset to zero!");
				ratings.setBackgroundColor(Color.GRAY);
				//pgDrawable.getPaint().setColor(Color.GRAY);
				ratings.setProgress(0);
			}
		}
	}
	
	private class LoadLikesTask extends AsyncTask<Void, Void, JSONArray> {
		
		protected JSONArray doInBackground(Void...arg0) {
			String url = "http://billiterate.pythonanywhere.com/billapp/bills?id=" + billId;
			System.err.println("URL = " + url);
			HttpResponse response;
			HttpClient client = new DefaultHttpClient();
			String responseString = "";
			
			try {
				response = client.execute(new HttpGet(url));
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();
				} else {
					response.getEntity().getContent().close();
				}
			} catch (ClientProtocolException cpe) {
				cpe.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			try {
				JSONArray messages = new JSONArray(responseString);
				return messages;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		protected void onPostExecute(JSONArray messageList) {
			if (messageList == null) {
				likes = 0;
				dislikes = 0;
				ratings.setBackgroundColor(Color.GRAY);
				ratings.setProgress(0);
				System.err.println("This bill has not been liked/disliked before, should display gray bar");
			} else {
				for (int i = 0; i < messageList.length(); i++) {
					try {
						JSONObject current = messageList.getJSONObject(i);
						JSONObject fields = current.getJSONObject("fields");
						likes = fields.getInt("num_likes");
						dislikes = fields.getInt("num_dislikes");
						System.out.println("Likes: " + likes + " Dislikes: " + dislikes);
					} catch (JSONException e ) {
						System.err.println(messageList.toString());
						e.printStackTrace();
					}
				}
				int total = likes + dislikes;
				if (total > 0) {
					int up = (likes * 100) / total;
					ratings.setBackgroundColor(Color.RED);
					pgDrawable.getPaint().setColor(Color.GREEN);
					System.err.println("progress bar should be green and up to: " + up);
					ratings.setProgress(up);
				}
			}
		}
	}
	
	public void postComment(View v) {
		/*TextView commentText = new TextView(this);
		commentText.setText(commentBox.getText());
		commentText.setLayoutParams(new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT ));
		commentText.setBackgroundDrawable(BillInfoActivity.this.getResources().getDrawable(R.drawable.black_border));
		bill_view.addView(commentText);*/
		String commentText = commentBox.getText().toString();
		String name = "Anonymous"; // Will eventually be populated by Facebook login
		PostTask post = new PostTask();
		post.execute(Integer.toString(billId), name, commentText);
		//System.err.println("bill ID = " + Integer.toString(billId));
		InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE); 

		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                   InputMethodManager.HIDE_NOT_ALWAYS);
		
		commentBox.setText("");
	}
	
	private class PostTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String...params) {
			String url = "http://billiterate.pythonanywhere.com/messages/" + Integer.toString(billId);
			HttpResponse response;
			HttpClient client = new DefaultHttpClient();
			try {
				HttpPost post = new HttpPost(url);
				List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
				postParameters.add(new BasicNameValuePair("bill", params[0]));
				postParameters.add(new BasicNameValuePair("name", params[1]));
				postParameters.add(new BasicNameValuePair("comment", params[2]));
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
				post.setEntity(entity);
				response = client.execute(post);
				System.err.println("Posting " + postParameters.toString() + "to " + url);
			} catch (ClientProtocolException cpe) {
				System.err.println("*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^");
				cpe.printStackTrace();
			} catch (IOException e) {
				System.err.println("*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^");
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String arg0) {
			load(null);
		}
	}
	
	private class LoadTask extends AsyncTask<Void, Void, JSONArray> {
		
		protected JSONArray doInBackground(Void...arg0) {
			String url = "http://billiterate.pythonanywhere.com/messages/" + Integer.toString(billId);
			System.err.println("URL = " + url);
			HttpResponse response;
			HttpClient client = new DefaultHttpClient();
			String responseString = "";
			
			try {
				response = client.execute(new HttpGet(url));
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					response.getEntity().writeTo(out);
					out.close();
					responseString = out.toString();
				} else {
					response.getEntity().getContent().close();
				}
			} catch (ClientProtocolException cpe) {
				cpe.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			try {
				JSONArray messages = new JSONArray(responseString);
				return messages;
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		protected void onPostExecute(JSONArray messageList) {
			data.clear();
			if (messageList == null) {
				return;
			}
			for (int i = 0; i < messageList.length(); i++) {
				try {
					JSONArray current = messageList.getJSONArray(i);
					Map<String, String> listItem = new HashMap<String, String>(2);
					listItem.put("ID", current.getString(0));
					listItem.put("Name", current.getString(1));
					listItem.put("Comment", current.getString(2));
					data.add(listItem);
				} catch (JSONException e ) {
					System.err.print(data.toString());
					e.printStackTrace();
				}
			}
			adapter.notifyDataSetChanged();
		}
	}
}
