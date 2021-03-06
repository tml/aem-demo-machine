/*******************************************************************************
 * Copyright 2015 Adobe Systems Incorporated.
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
 ******************************************************************************/
package com.adobe.aem.demo.communities;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class Loader {

	static Logger logger = Logger.getLogger(Loader.class);

	private static final String USERS = "Users";
	private static final String COMMENTS = "Comments";
	private static final String REVIEWS = "Reviews";
	private static final String RATINGS = "Ratings";
	private static final String FORUM = "Forum";
	private static final String JOURNAL = "Journal";
	private static final String TAG = "Tag";
	private static final String BLOG = "Blog";
	private static final String SUMMARY = "Summary";
	private static final String CALENDAR = "Calendar";
	private static final String PREFERENCES = "Preferences";
	private static final String FILES = "Files";
	private static final String IMAGE = "file";
	private static final String AVATAR = "Avatar";
	private static final String QNA = "QnA";
	private static final String ACTIVITIES = "Activities";
	private static final String SLINGPOST = "SlingPost";
	private static final String SLINGDELETE = "SlingDelete";
	private static final String PASSWORD = "password";
	private static final String SITE = "Site";
	private static final String SITETEMPLATE = "SiteTemplate";
	private static final String GROUPMEMBERS = "GroupMembers";
	private static final String SITEMEMBERS = "SiteMembers";
	private static final String GROUP = "Group";
	private static final String JOIN = "Join";
	private static final String ASSET = "Asset";
	private static final String KILL = "Kill";
	private static final String MESSAGE = "Message";
	private static final String RESOURCE = "Resource";
	private static final int RESOURCE_INDEX_PATH = 5;
	private static final int RESOURCE_INDEX_THUMBNAIL = 3;
	private static final int ASSET_INDEX_NAME = 4;
	private static final int RESOURCE_INDEX_SITE = 7;
	private static final int RESOURCE_INDEX_FUNCTION = 9;
	private static final int RESOURCE_INDEX_PROPERTIES = 10;
	private static final int GROUP_INDEX_NAME = 1;
	private static final String FOLLOW = "Follow";
	private static final String LEARNING = "LearningPath";
	private static final String BANNER = "pagebanner";
	private static final String THUMBNAIL = "pagethumbnail";
	private static final String LANGUAGE = "baseLanguage";
	private static final int MAXRETRIES=30;
	private static final int REPORTINGDAYS=-21;
	private static String[] comments = {"This course deserves some improvements", "The conclusion is not super clear", "Very crisp, love it", "Interesting, but I need to look at this course again", "Good course, I'll recommend it.", "Really nice done. Sharing with my peers", "Excellent course. Giving it a top rating."};
	
	public static void main(String[] args) {

		String hostname = null;
		String port = null;
		String altport = null;
		String csvfile = null;
		String location = null;
		String language = "en";
		String analytics = null;
		String adminPassword = "admin";
		String[] url = new String[10];  // Handling 10 levels maximum for nested comments 
		boolean reset = false;
		boolean configure = false;
		int urlLevel = 0;
		int row = 0;
		HashMap<String,ArrayList<String>> learningpaths=new HashMap<String,ArrayList<String>>();

		// Command line options for this tool
		Options options = new Options();
		options.addOption("h", true, "Hostname");
		options.addOption("p", true, "Port");
		options.addOption("a", true, "Alternate Port");
		options.addOption("f", true, "CSV file");
		options.addOption("r", false, "Reset");
		options.addOption("u", true, "Admin Password");
		options.addOption("c", false, "Configure");
		options.addOption("s", true, "Analytics Endpoint");
		options.addOption("t", false, "Analytics Tracking");
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse( options, args);

			if(cmd.hasOption("h")) {
				hostname = cmd.getOptionValue("h");
			}

			if(cmd.hasOption("p")) {
				port = cmd.getOptionValue("p");
			}

			if(cmd.hasOption("a")) {
				altport = cmd.getOptionValue("a");
			}

			if(cmd.hasOption("f")) {
				csvfile = cmd.getOptionValue("f");
			}

			if(cmd.hasOption("u")) {
				adminPassword = cmd.getOptionValue("u");
			}

			if(cmd.hasOption("t")) {
				if(cmd.hasOption("s")) {
					analytics = cmd.getOptionValue("s");
				}
			}

			if(cmd.hasOption("r")) {
				reset = true;
			}

			if(cmd.hasOption("c")) {
				configure = true;
			}


			if (csvfile==null || port == null || hostname == null) {
				System.out.println("Request parameters: -h hostname -p port -a alternateport -u adminPassword -f path_to_CSV_file -r (true|false, delete content before import) -c (true|false, post additional properties)");
				System.exit(-1);
			}

		} catch (ParseException ex) {

			logger.error(ex.getMessage());

		}

		String componentType = null;

		try {
			
			logger.debug("AEM Demo Loader: Processing file " + csvfile);

			// Reading the CSV file, line by line
			Reader in = new FileReader(csvfile);

			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			for (CSVRecord record : records) {

				row = row + 1;
				logger.info("Row: " + row + ", new record: " + record.get(0));			

				// Let's see if we deal with a comment
				if (record.get(0).startsWith("#")) {

					// We can ignore the comment line and move on
					continue;

				}

				// Let's see if we need to terminate this process
				if (record.get(0).equals(KILL)) {

					System.exit(1);

				}

				// Let's see if we need to create a new Community site
				if (record.get(0).equals(SITE)) {

					// Building the form entity to be posted
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.setCharset(MIME.UTF8_CHARSET);
					builder.addTextBody(":operation", "social:createSite", ContentType.create("text/plain", MIME.UTF8_CHARSET));
					builder.addTextBody("_charset_", "UTF-8", ContentType.create("text/plain", MIME.UTF8_CHARSET));

					String urlName = null;

					for (int i=2;i<record.size()-1;i=i+2) {

						if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).length()>0) {

							String name = record.get(i).trim();
							String value = record.get(i+1).trim();
							if (value.equals("TRUE")) { value = "true"; }
							if (value.equals("FALSE")) { value = "false"; }	
							if (name.equals("urlName")) { urlName = value; }
							if (name.equals(LANGUAGE)) { language = value; }
							if (name.equals(BANNER)) {

								File attachment = new File(csvfile.substring(0, csvfile.indexOf(".csv")) + File.separator + value);
								builder.addBinaryBody(BANNER, attachment, ContentType.MULTIPART_FORM_DATA, attachment.getName());

							} else if (name.equals(THUMBNAIL)) {

								File attachment = new File(csvfile.substring(0, csvfile.indexOf(".csv")) + File.separator + value);
								builder.addBinaryBody(THUMBNAIL, attachment, ContentType.MULTIPART_FORM_DATA, attachment.getName());

							} else {

								builder.addTextBody(name, value, ContentType.create("text/plain", MIME.UTF8_CHARSET));

							}
						}
					}

					// Site creation
					String siteId = doPost(hostname, port,
							"/content.social.json",
							"admin", adminPassword,
							builder.build(),
							"response/siteId");

					// Site publishing, if there's a publish instance to publish to
					if (!port.equals(altport)) {

						List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
						nameValuePairs.add(new BasicNameValuePair("id", "nobot"));
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:publishSite"));
						nameValuePairs.add(new BasicNameValuePair("path", "/content/sites/" + urlName + "/" + language));

						doPost(hostname, port,
								"/communities/sites.html",
								"admin", adminPassword,
								new UrlEncodedFormEntity(nameValuePairs),
								null);

						// Wait for site to be available on Publish
						doWait(hostname, altport,
								"admin", adminPassword,
								(siteId!=null?siteId:urlName) + "-groupadministrators");
					}

					continue;
				}

				// Let's see if we need to create a new Tag
				if (record.get(0).equals(TAG)) {

					// Building the form entity to be posted
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.setCharset(MIME.UTF8_CHARSET);
					builder.addTextBody("_charset_", "UTF-8", ContentType.create("text/plain", MIME.UTF8_CHARSET));

					for (int i=1;i<record.size()-1;i=i+2) {

						if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).length()>0 && record.get(i+1).length()>0) {

							String name = record.get(i).trim();
							String value = record.get(i+1).trim();
							builder.addTextBody(name, value, ContentType.create("text/plain", MIME.UTF8_CHARSET));

						}
					}

					// Tag creation
					doPost(hostname, port,
							"/bin/tagcommand",
							"admin", adminPassword,
							builder.build(),
							null);

					continue;
				}

				// Let's see if we need to create a new Community site template, and if we can do it (script run against author instance)
				if (record.get(0).equals(SITETEMPLATE)) {

					// Building the form entity to be posted
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.setCharset(MIME.UTF8_CHARSET);
					builder.addTextBody(":operation", "social:createSiteTemplate", ContentType.create("text/plain", MIME.UTF8_CHARSET));
					builder.addTextBody("_charset_", "UTF-8", ContentType.create("text/plain", MIME.UTF8_CHARSET));

					for (int i=2;i<record.size()-1;i=i+2) {

						if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).length()>0) {

							String name = record.get(i).trim();
							String value = record.get(i+1).trim();
							builder.addTextBody(name, value, ContentType.create("text/plain", MIME.UTF8_CHARSET));

						}
					}

					// Site template creation
					doPost(hostname, port,
							"/content.social.json",
							"admin", adminPassword,
							builder.build(),
							null);

					continue;
				}

				// Let's see if we need to create a new Community group
				if (record.get(0).equals(GROUP)) {

					// Building the form entity to be posted
					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
					builder.setCharset(MIME.UTF8_CHARSET);
					builder.addTextBody(":operation", "social:createCommunityGroup", ContentType.create("text/plain", MIME.UTF8_CHARSET));
					builder.addTextBody("_charset_", "UTF-8", ContentType.create("text/plain", MIME.UTF8_CHARSET));

					for (int i=3;i<record.size()-1;i=i+2) {

						if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).length()>0) {

							String name = record.get(i).trim();
							String value = record.get(i+1).trim();
							if (value.equals("TRUE")) { value = "true"; }
							if (value.equals("FALSE")) { value = "false"; }	
							if (name.equals(IMAGE)) {

								File attachment = new File(csvfile.substring(0, csvfile.indexOf(".csv")) + File.separator + value);
								builder.addBinaryBody(IMAGE, attachment, ContentType.MULTIPART_FORM_DATA, attachment.getName());

							} else {

								builder.addTextBody(name, value, ContentType.create("text/plain", MIME.UTF8_CHARSET));

							}
						}
					}

					// Group creation
					String memberGroupId = doPost(hostname, port,
							record.get(1),
							getUserName(record.get(2)), getPassword(record.get(2), adminPassword),
							builder.build(),
							"response/memberGroupId");

					// Wait for group to be available on Publish, if available
					logger.debug("Waiting for completion of Community Group creation");
					doWait(hostname, port,
								"admin", adminPassword,
								memberGroupId);

					continue;

				}

				// Let's see if it's simple Sling Delete request
				if (record.get(0).equals(SLINGDELETE)) {

					doDelete(hostname, port,
							record.get(1),
							"admin", adminPassword);

					continue;

				}

				// Let's see if we need to add users to an AEM Group
				if ((record.get(0).equals(GROUPMEMBERS) || record.get(0).equals(SITEMEMBERS)) && record.get(GROUP_INDEX_NAME)!=null) {

					// Checking if we have a member group for this site
					String groupName = record.get(GROUP_INDEX_NAME);
					if (record.get(0).equals(SITEMEMBERS)) {

						// Let's fetch the siteId for this Community Site Url
						String siteConfig = doGet(hostname, port,
								groupName,
								"admin",adminPassword,
								null);

						try {
							
							String siteId = new JSONObject(siteConfig).getString("siteId");
							if (siteId!=null) groupName = "community-" + siteId + "-members";
							logger.debug("Member group name is " + groupName);
							
						} catch (Exception e) {
							
							logger.error(e.getMessage());
							
						}
												
					}

					// Pause until the group can found
					doWait(hostname, port,
							"admin", adminPassword,
							groupName
							);

					List<NameValuePair>  nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("filter", "[{\"operation\":\"like\",\"rep:principalName\":\"" + groupName + "\"}]"));
					nameValuePairs.add(new BasicNameValuePair("type", "groups"));
					String groupList = doGet(hostname, port,
							"/libs/social/console/content/content/userlist.social.0.10.json",
							"admin",adminPassword,
							nameValuePairs);

					logger.debug("List of groups" + groupList);

					if (groupList.indexOf(groupName)>0) {

						logger.debug("Group was found on " + port);
						try {
							JSONArray jsonArray = new JSONObject(groupList).getJSONArray("items");
							if (jsonArray.length()==1) {
								JSONObject jsonObject = jsonArray.getJSONObject(0);
								String groupPath= jsonObject.getString("path");

								logger.debug("Group path is " + groupPath);

								// Constructing a multi-part POST for group membership
								MultipartEntityBuilder builder = MultipartEntityBuilder.create();
								builder.setCharset(MIME.UTF8_CHARSET);
								builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

								List<NameValuePair> groupNameValuePairs = buildNVP(record, 2);
								for (NameValuePair nameValuePair : groupNameValuePairs) {
									builder.addTextBody(nameValuePair.getName(), nameValuePair.getValue(), ContentType.create("text/plain", MIME.UTF8_CHARSET));
								}

								// Adding the list of group members
								doPost(hostname, port,
										groupPath + ".rw.userprops.html",
										"admin", adminPassword,
										builder.build(),
										null);

							} else {
								logger.info("We have more than one match for a group with this name!");			
							}
						} catch (Exception e) {
							logger.error(e.getMessage());
						}
					}

					continue;

				}

				// Let's see if it's user related
				if (record.get(0).equals(USERS)) {

					//First we need to get the path to the user node
					String json = doGet(hostname, port,
							"/libs/granite/security/currentuser.json",
							getUserName(record.get(1)), getPassword(record.get(1), adminPassword),
							null);

					if (json!=null) {

						try {

							// Fetching the home property
							String home = new JSONObject(json).getString("home");
							if (record.get(2).equals(PREFERENCES)) {
								home = home + "/preferences";
							} else {
								home = home + "/profile";
							}
							logger.debug(home);

							// Now we can post all the preferences or the profile
							List<NameValuePair> nameValuePairs = buildNVP(record, 3);
							doPost(hostname, port,
									home,
									"admin", adminPassword,
									new UrlEncodedFormEntity(nameValuePairs),
									null);

						} catch (Exception e) {
							logger.error(e.getMessage());
						}

					}

					continue;

				}

				// Let's see if we deal with a new block of content or just a new entry
				if (record.get(0).equals(CALENDAR) 
						|| record.get(0).equals(SLINGPOST)
						|| record.get(0).equals(RATINGS) 
						|| record.get(0).equals(BLOG) 
						|| record.get(0).equals(JOURNAL) 
						|| record.get(0).equals(COMMENTS) 
						|| record.get(0).equals(REVIEWS) 
						|| record.get(0).equals(FILES) 
						|| record.get(0).equals(SUMMARY) 
						|| record.get(0).equals(ACTIVITIES) 
						|| record.get(0).equals(JOIN) 
						|| record.get(0).equals(FOLLOW) 
						|| record.get(0).equals(MESSAGE) 
						|| record.get(0).equals(ASSET) 
						|| record.get(0).equals(AVATAR) 
						|| record.get(0).equals(RESOURCE) 
						|| record.get(0).equals(LEARNING) 
						|| record.get(0).equals(QNA) 
						|| record.get(0).equals(FORUM)) {

					// New block of content, we need to reset the processing to first Level
					componentType = record.get(0);
					url[0] = record.get(1);
					urlLevel=0;

					if (!componentType.equals(SLINGPOST) && reset) {

						int pos = record.get(1).indexOf("/jcr:content");
						if (pos>0) 
							doDelete(hostname, port,
									"/content/usergenerated" + record.get(1).substring(0,pos),
									"admin", adminPassword);

					}

					// If the Configure command line flag is set, we try to configure the component with all options enabled
					if (componentType.equals(SLINGPOST) || configure) {

						String configurePath = getConfigurePath(record.get(1));

						List<NameValuePair> nameValuePairs = buildNVP(record, 2);
						if (nameValuePairs.size()>2)    // Only do this when really have configuration settings
							doPost(hostname, port,
									configurePath,
									"admin", adminPassword,
									new UrlEncodedFormEntity(nameValuePairs),
									null);

					}

					// We're done with this line, moving on to the next line in the CSV file
					continue;
				}

				// Let's see if we need to indent the list, if it's a reply or a reply to a reply
				if (record.get(1).length()!=1) continue;  // We need a valid level indicator

				if (Integer.parseInt(record.get(1))>urlLevel) {
					url[++urlLevel] = location;
					logger.debug("Incremented urlLevel to: " + urlLevel + ", with a new location:" + location);
				} else if (Integer.parseInt(record.get(1))<urlLevel) {
					urlLevel = Integer.parseInt(record.get(1));
					logger.debug("Decremented urlLevel to: " + urlLevel);
				}

				// Get the credentials or fall back to password
				String password = getPassword(record.get(0), adminPassword);
				String userName = getUserName(record.get(0));

				// Adding the generic properties for all POST requests
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

				if (!componentType.equals(RESOURCE))
					nameValuePairs.add(new BasicNameValuePair("id", "nobot"));

				nameValuePairs.add(new BasicNameValuePair("_charset_", "UTF-8"));

				// Setting some specific fields depending on the content type
				if (componentType.equals(COMMENTS)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:createComment"));
					nameValuePairs.add(new BasicNameValuePair("message", record.get(2)));

				}

				// Creates a forum post (or reply)
				if (componentType.equals(FORUM)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:createForumPost"));
					nameValuePairs.add(new BasicNameValuePair("subject", record.get(2)));
					nameValuePairs.add(new BasicNameValuePair("message", record.get(3)));		         

				}

				// Follows a user (followedId) for the user posting the request
				if (componentType.equals(FOLLOW)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:follow"));
					nameValuePairs.add(new BasicNameValuePair("userId", "/social/authors/" + userName));
					nameValuePairs.add(new BasicNameValuePair("followedId", "/social/authors/" + record.get(2)));

				}

				// Uploading Avatar picture
				if (componentType.equals(AVATAR)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:changeAvatar"));

				}

				// Joins a user (posting the request) to a Community Group (path)
				if (componentType.equals(JOIN)) {
					nameValuePairs.add(new BasicNameValuePair(":operation", "social:joinCommunityGroup"));
					int pos = url[0].indexOf("/configuration.social.json");
					if (pos>0)
						nameValuePairs.add(new BasicNameValuePair("path", url[0].substring(0,pos) + ".html"));
					else
						continue; // Invalid record
				}

				// Creates a new private message
				if (componentType.equals(MESSAGE)) {
					nameValuePairs.add(new BasicNameValuePair(":operation", "social:createMessage"));
					nameValuePairs.add(new BasicNameValuePair("sendMail", "Sending..."));
					nameValuePairs.add(new BasicNameValuePair("content", record.get(4)));
					nameValuePairs.add(new BasicNameValuePair("subject", record.get(3)));
					nameValuePairs.add(new BasicNameValuePair("serviceSelector", "/bin/community"));
					nameValuePairs.add(new BasicNameValuePair("to", "/social/authors/" + record.get(2)));
					nameValuePairs.add(new BasicNameValuePair("userId", "/social/authors/" + record.get(2)));
					nameValuePairs.add(new BasicNameValuePair(":redirect", "//messaging.html"));
					nameValuePairs.add(new BasicNameValuePair(":formid", "generic_form"));
					nameValuePairs.add(new BasicNameValuePair(":formstart", "/content/sites/communities/messaging/compose/jcr:content/content/primary/start"));
				}

				// Creates a file or a folder
				if (componentType.equals(FILES)) {

					// Top level is always assumed to be a folder, second level files, and third and subsequent levels comments on files
					if (urlLevel==0) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createFileLibraryFolder"));
						nameValuePairs.add(new BasicNameValuePair("name", record.get(2)));
						nameValuePairs.add(new BasicNameValuePair("message", record.get(3)));		         
					} else if (urlLevel==1) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createComment"));
					}

				}

				// Creates a question, a reply or mark a reply as the best answer
				if (componentType.equals(QNA)) {
					if (urlLevel == 0) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createQnaPost"));
						nameValuePairs.add(new BasicNameValuePair("subject", record.get(2)));
						nameValuePairs.add(new BasicNameValuePair("message", record.get(3)));
					} else if (urlLevel == 1) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createQnaPost"));
						nameValuePairs.add(new BasicNameValuePair("message", record.get(3)));
					} else if (urlLevel == 2) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:selectAnswer"));	            	   
					}
				}

				// Creates an article or a comment
				if (componentType.equals(JOURNAL) || componentType.equals(BLOG)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:createJournalComment"));
					nameValuePairs.add(new BasicNameValuePair("subject", record.get(2)));
					StringBuffer message = new StringBuffer("<p>" + record.get(3) + "</p>");

					//We might have more paragraphs to add to the blog or journal article
					for (int i=6; i < record.size();i++) {
						if (record.get(i).length()>0) {
							message.append("<p>" + record.get(i) + "</p>");
						}
					}
					
					//We might have some tags to add to the blog or journal article
					if (record.get(5).length()>0) {
						nameValuePairs.add(new BasicNameValuePair("tags", record.get(5)));		         				
					}
					
					nameValuePairs.add(new BasicNameValuePair("message", message.toString()));		         

				}

				// Creates a review or a comment
				if (componentType.equals(REVIEWS)) {

					nameValuePairs.add(new BasicNameValuePair("message", record.get(2)));

					// This might be a top level review, or a comment on a review or another comment
					if (urlLevel==0) {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createReview"));
						nameValuePairs.add(new BasicNameValuePair("ratings", record.get(3)));
						if (record.size()>4 &&
								record.get(4).length()>0) {
							// If we are dealing with a non-existent resource, then the design drives the behavior
							nameValuePairs.add(new BasicNameValuePair("scf:resourceType", "social/reviews/components/hbs/reviews"));
							nameValuePairs.add(new BasicNameValuePair("scf:included",record.get(4)));							
						}
					} else {
						nameValuePairs.add(new BasicNameValuePair(":operation", "social:createComment"));
					}

				}

				// Creates a rating
				if (componentType.equals(RATINGS)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:postTallyResponse"));
					nameValuePairs.add(new BasicNameValuePair("tallyType", "Rating"));
					nameValuePairs.add(new BasicNameValuePair("response", record.get(2)));

				}

				// Creates a DAM asset
				if (componentType.equals(ASSET) && record.get(ASSET_INDEX_NAME).length()>0) {

					nameValuePairs.add(new BasicNameValuePair("fileName", record.get(ASSET_INDEX_NAME)));

				}

				// Creates an enablement resource
				if (componentType.equals(RESOURCE)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "se:createResource"));

					List<NameValuePair> otherNameValuePairs = buildNVP(record, RESOURCE_INDEX_PROPERTIES);
					nameValuePairs.addAll(otherNameValuePairs);

					// Adding the site
					nameValuePairs.add(new BasicNameValuePair("site", "/content/sites/" + record.get(RESOURCE_INDEX_SITE) + "/resources/en"));

					// Building the cover image fragment
					if (record.get(RESOURCE_INDEX_THUMBNAIL).length()>0) {
						nameValuePairs.add(new BasicNameValuePair("cover-image", doThumbnail(hostname, port, adminPassword, csvfile, record.get(RESOURCE_INDEX_THUMBNAIL))));
					} else {
						nameValuePairs.add(new BasicNameValuePair("cover-image", ""));			
					}

					// Building the asset fragment
					String coverPath = "/content/dam/" + record.get(RESOURCE_INDEX_SITE) + "/resource-assets/" + record.get(2) + "/jcr:content/renditions/cq5dam.thumbnail.319.319.png";
					String coverSource = "dam";
					String assets = "[{\"cover-img-path\":\"" + coverPath + "\",\"thumbnail-source\":\"" + coverSource + "\",\"asset-category\":\"enablementAsset:dam\",\"resource-asset-name\":null,\"state\":\"A\",\"asset-path\":\"/content/dam/" + record.get(RESOURCE_INDEX_SITE) + "/resource-assets/" + record.get(2) + "\"}]";
					nameValuePairs.add(new BasicNameValuePair("assets", assets));

					logger.debug("assets:" + assets);

				}

				// Creates a learning path
				if (componentType.equals(LEARNING)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "se:editLearningPath"));

					List<NameValuePair> otherNameValuePairs = buildNVP(record, RESOURCE_INDEX_PROPERTIES);
					nameValuePairs.addAll(otherNameValuePairs);

					// Adding the site
					nameValuePairs.add(new BasicNameValuePair("site", "/content/sites/" + record.get(RESOURCE_INDEX_SITE) + "/resources/en"));

					// Building the cover image fragment
					if (record.get(RESOURCE_INDEX_THUMBNAIL).length()>0) {
						nameValuePairs.add(new BasicNameValuePair("card-image", doThumbnail(hostname, port, adminPassword, csvfile, record.get(RESOURCE_INDEX_THUMBNAIL))));
					}

					// Building the learning path fragment
					StringBuffer assets = new StringBuffer("[\"");
					if (learningpaths.get(record.get(2)) != null) {

						ArrayList<String> paths = learningpaths.get(record.get(2));
						int i=0;
						for (String path : paths) {
							assets.append("{\\\"type\\\":\\\"linked-resource\\\",\\\"path\\\":\\\"");
							assets.append(path);
							assets.append("\\\"}");
							if (i++<paths.size()-1) { assets.append("\",\""); }
						}						

					} else {						
						logger.debug("No asset for this learning path");
					}
					assets.append("\"]");
					nameValuePairs.add(new BasicNameValuePair("learningpath-items", assets.toString()));
					logger.debug("Learning path:" + assets.toString());

				}

				// Creates a calendar event
				if (componentType.equals(CALENDAR)) {

					nameValuePairs.add(new BasicNameValuePair(":operation", "social:createEvent"));
					try {
						JSONObject event = new JSONObject();

						// Building the JSON fragment for a new calendar event
						event.accumulate("subject", record.get(2));
						event.accumulate("message", record.get(3));
						event.accumulate("location", record.get(4));
						event.accumulate("tags", "");
						event.accumulate("undefined", "update");

						String startDate = record.get(5);
						startDate = startDate.replaceAll("YYYY", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
						startDate = startDate.replaceAll("MM", Integer.toString(1+Calendar.getInstance().get(Calendar.MONTH)));
						event.accumulate("start", startDate);

						String endDate = record.get(6);
						endDate = endDate.replaceAll("YYYY", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
						endDate = endDate.replaceAll("MM", Integer.toString(1+Calendar.getInstance().get(Calendar.MONTH)));
						event.accumulate("end",endDate);
						nameValuePairs.add(new BasicNameValuePair("event",event.toString()));

					} catch(Exception ex) {

						logger.error(ex.getMessage());

					}

				}

				// Constructing a multi-part POST request
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				builder.setCharset(MIME.UTF8_CHARSET);
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);				
				for (NameValuePair nameValuePair : nameValuePairs) {
					builder.addTextBody(nameValuePair.getName(), nameValuePair.getValue(), ContentType.create("text/plain", MIME.UTF8_CHARSET));
				}

				// See if we have attachments for this new post - or some other actions require a form nonetheless
				if ((componentType.equals(ASSET) || componentType.equals(AVATAR) || componentType.equals(FORUM) || (componentType.equals(JOURNAL)) || componentType.equals(BLOG)) && record.size()>4 && record.get(ASSET_INDEX_NAME).length()>0) {

					File attachment = new File(csvfile.substring(0, csvfile.indexOf(".csv")) + File.separator + record.get(ASSET_INDEX_NAME));

					ContentType ct = ContentType.MULTIPART_FORM_DATA;
					if (record.get(ASSET_INDEX_NAME).indexOf(".mp4")>0) {
						ct = ContentType.create("video/mp4", MIME.UTF8_CHARSET);
					} else if (record.get(ASSET_INDEX_NAME).indexOf(".jpg")>0 || record.get(ASSET_INDEX_NAME).indexOf(".jpeg")>0) {
						ct = ContentType.create("image/jpeg", MIME.UTF8_CHARSET);
					} else if (record.get(ASSET_INDEX_NAME).indexOf(".png")>0) {
						ct = ContentType.create("image/png", MIME.UTF8_CHARSET);
					} else if (record.get(ASSET_INDEX_NAME).indexOf(".pdf")>0) {
						ct = ContentType.create("application/pdf", MIME.UTF8_CHARSET);
					} else if (record.get(ASSET_INDEX_NAME).indexOf(".zip")>0) {
						ct = ContentType.create("application/zip", MIME.UTF8_CHARSET);
					}
					builder.addBinaryBody("file", attachment, ct, attachment.getName());
					logger.debug("Adding file to payload with name: " + attachment.getName() + " and type: " + ct.getMimeType());

				}

				// If it's a resource or a learning path, we need the path to the resource for subsequent publishing
				String jsonElement = "location";
				if (componentType.equals(RESOURCE)) {
					jsonElement = "changes/argument";
				}
				if (componentType.equals(LEARNING)) {
					jsonElement = "path";
				}
				if (componentType.equals(ASSET)) {
					jsonElement = null;
				}

				// This call generally returns the path to the content fragment that was just created
				location = Loader.doPost(hostname, port,
						url[urlLevel],
						userName, password,
						builder.build(),
						jsonElement);

				// If we are loading a DAM asset, we are waiting for all renditions to be generated before proceeding
				if (componentType.equals(ASSET)) {
					int pathIndex = url[urlLevel].lastIndexOf(".createasset.html");
					if (pathIndex>0)
						doWaitPath(hostname, port, adminPassword, url[urlLevel].substring(0, pathIndex) + "/" + record.get(ASSET_INDEX_NAME) + "/jcr:content/renditions", "nt:file");
				}

				// Let's see if it needs to be added to a learning path
				if (componentType.equals(RESOURCE) && record.get(RESOURCE_INDEX_PATH).length()>0 && location!=null) {

					// Adding the location to a list of a resources for this particular Learning Path
					if (learningpaths.get(record.get(RESOURCE_INDEX_PATH)) == null) learningpaths.put(record.get(RESOURCE_INDEX_PATH), new ArrayList<String>());
					logger.debug("Adding resource to Learning path: " + record.get(RESOURCE_INDEX_PATH));
					ArrayList<String> locations = learningpaths.get(record.get(RESOURCE_INDEX_PATH));
					locations.add(location);
					learningpaths.put(record.get(RESOURCE_INDEX_PATH), locations);

				}

				// If it's a Learning Path, we publish it when possible
				if (componentType.equals(LEARNING) && !port.equals(altport) && location!=null) {

					// Publishing the learning path 
					List<NameValuePair> publishNameValuePairs = new ArrayList<NameValuePair>();
					publishNameValuePairs.add(new BasicNameValuePair(":operation","se:publishEnablementContent"));
					publishNameValuePairs.add(new BasicNameValuePair("replication-action","activate"));
					logger.debug("Publishing a learning path from: " + location);					
					Loader.doPost(hostname, port,
							location,
							userName, password,
							new UrlEncodedFormEntity(publishNameValuePairs),
							null);

					// Waiting for the learning path to be published
					Loader.doWait(hostname, altport,
							"admin", adminPassword,
							location.substring(1 + location.lastIndexOf("/"))     // Only search for groups with the learning path in it
							);

					// Decorate the resources within the learning path with comments and ratings, randomly generated
					ArrayList<String> paths = learningpaths.get(record.get(2));
					for (String path : paths) {
						doDecorate(hostname, altport, path, record, analytics);
					}						

				}

				// If it's an Enablement Resource, a lot of things need to happen...
				// Step 1. If it's a SCORM resource, we wait for the SCORM metadata workflow to be complete before proceeding
				// Step 2. We publish the resource
				// Step 3. We set a new first published date on the resource (3 weeks earlier) so that reporting data is more meaningful
				// Step 4. We wait for the resource to be available on publish (checking that associated groups are available)
				// Step 5. We retrieve the json for the resource on publish to retrieve the Social endpoints
				// Step 6. We post ratings and comments for each of the enrollees on publish
				if (componentType.equals(RESOURCE) && !port.equals(altport) && location!=null) {

					// Wait for the data to be fully copied
					doWaitPath(hostname, port, adminPassword, location + "/assets/asset", "nt:file");

					// If we are dealing with a SCORM asset, we wait a little bit before publishing the resource to that the SCORM workflow is completed 
					if (record.get(2).indexOf(".zip")>0) {
						doSleep(10000, "SCORM Resource, waiting for workflow to complete");
					}

					// Publishing the resource 
					List<NameValuePair> publishNameValuePairs = new ArrayList<NameValuePair>();
					publishNameValuePairs.add(new BasicNameValuePair(":operation","se:publishEnablementContent"));
					publishNameValuePairs.add(new BasicNameValuePair("replication-action","activate"));
					logger.debug("Publishing a resource from: " + location);					
					Loader.doPost(hostname, port,
							location,
							userName, password,
							new UrlEncodedFormEntity(publishNameValuePairs),
							null);

					// Waiting for the resource to be published
					Loader.doWait(hostname, altport,
							"admin", adminPassword,
							location.substring(1 + location.lastIndexOf("/"))     // Only search for groups with the resource path in it
							);

					// Setting the first published timestamp so that reporting always comes with 3 weeks of data after building a new demo instance
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DATE, REPORTINGDAYS);    
					List<NameValuePair> publishDateNameValuePairs = new ArrayList<NameValuePair>();
					publishDateNameValuePairs.add(new BasicNameValuePair("date-first-published", dateFormat.format(cal.getTime())));
					logger.debug("Setting the publish date for a resource from: " + location);
					doPost(hostname, port,
							location,
							userName, password,
							new UrlEncodedFormEntity(publishDateNameValuePairs),
							null);

					// Adding comments and ratings for this resource
					doDecorate(hostname, altport, location, record, analytics);

				}				

			}
		} catch (IOException e) {

			logger.error(e.getMessage());

		}		

	}

	// This method extracts the user name for a record
	private static String getUserName(String record) {

		String userName = record;
		int pass = userName.indexOf("/");
		if (pass>0) {
			userName = userName.substring(0,pass);
		}
		return userName;

	}

	// This method extracts the password for a record
	private static String getPassword(String record, String adminPassword) {

		String defaultPassword = PASSWORD;
		
		// If this is the AEM admin user, always return the configured admin password
		if (getUserName(record).equals("admin")) {
			return adminPassword;
		}
		
		// If not and if a password is provided in the CSV record, return this password
		int pass = record.indexOf("/");
		if (pass>0) {
			return record.substring(pass+1);
		}	
		
		// If not, return the defaut password 
		return defaultPassword;

	}

	// This method gets the configuration path for a record
	private static String getConfigurePath(String record) {

		String configurePath = record;
		int json = configurePath.indexOf(".social.json");
		if (json>0) {
			configurePath = configurePath.substring(0,json);
		}
		return configurePath;

	}

	// This method waits a little bit
	private static void doSleep(long ms, String message) {

		// Wait 2 seconds
		try {
			logger.debug("Waiting " + ms + " milliseconds: " + message);
			Thread.sleep(ms);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

	}

	// This method POSTs a file to be used as a thumbnail later on
	private static String doThumbnail(String hostname, String port, String adminPassword, String csvfile, String filename) {

		String pathToFile = "/content/dam/communities/resource-thumbnails/" + filename;
		File attachment = new File(csvfile.substring(0, csvfile.indexOf(".csv")) + File.separator + filename);

		ContentType ct = ContentType.MULTIPART_FORM_DATA;
		if (filename.indexOf(".mp4")>0) {
			ct = ContentType.create("video/mp4", MIME.UTF8_CHARSET);
		} else if (filename.indexOf(".jpg")>0 || filename.indexOf(".jpeg")>0) {
			ct = ContentType.create("image/jpeg", MIME.UTF8_CHARSET);
		} else if (filename.indexOf(".png")>0) {
			ct = ContentType.create("image/png", MIME.UTF8_CHARSET);
		}

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setCharset(MIME.UTF8_CHARSET);
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);				
		builder.addBinaryBody("file", attachment, ct, attachment.getName());
		builder.addTextBody("fileName", filename, ContentType.create("text/plain", MIME.UTF8_CHARSET));

		logger.debug("Adding file for thumbnails with name: " + attachment.getName() + " and type: " + ct.getMimeType());

		Loader.doPost(hostname, port,
				pathToFile,
				"admin", adminPassword,
				builder.build(),
				null);

		logger.debug("Path to thumbnail: " + pathToFile);

		return pathToFile + "/file";

	}

	// This method POSTs a set of comments and ratings for a resource for a particular location
	private static void doDecorate(String hostname, String altport, String location, CSVRecord record, String analytics) {

		// Getting the JSON view of the resource
		String resourceJson = Loader.doGet(hostname, altport,
				location + ".social.json",
				"admin","admin",
				null);
		logger.debug("JSON view of the resource is: " + resourceJson);					

		// Generating random ratings and comments for the resource for each of the enrolled users
		try {

			JSONObject resourceJsonObject = new JSONObject(resourceJson);

			String resourceRatingsEndpoint = resourceJsonObject.getString("ratingsEndPoint");
			String resourceCommentsEndpoint = resourceJsonObject.getString("commentsEndPoint");
			String resourceID = resourceJsonObject.getString("id");
			String resourceType = resourceJsonObject.getJSONObject("assetProperties").getString("type");
			String referer = "http://localhost:" + altport + "/content/sites/" + record.get(RESOURCE_INDEX_SITE) + "/en" + (record.get(RESOURCE_INDEX_FUNCTION).length()>0?("/" + record.get(RESOURCE_INDEX_FUNCTION)):"") + ".resource.html" + resourceID; 

			logger.debug("Resource Ratings Endpoint: " + resourceRatingsEndpoint);
			logger.debug("Resource Comments Endpoint: " + resourceCommentsEndpoint);
			logger.debug("Resource Type: " + resourceType);
			logger.debug("Resource ID: " + resourceID);
			logger.debug("Referer: " + referer);

			// Looking for the list of enrolled users
			for (int i=0;i<record.size()-1;i=i+1) {

				if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).equals("deltaList")) {

					JSONObject enrolledJsonObject = new JSONObject(record.get(i+1));
					Iterator<?> iter = enrolledJsonObject.keys();
					while (iter.hasNext()) {

						String key = (String) iter.next();
						logger.debug("New Resource Enrollee: " + key);

						// Getting information about this enrollee (user or group?)
						List<NameValuePair>  nameValuePairs = new ArrayList<NameValuePair>();
						nameValuePairs.add(new BasicNameValuePair("filter", "[{\"operation\":\"like\",\"rep:principalName\":\"" + key + "\"}]"));
						String list = Loader.doGet(hostname, altport,
								"/libs/social/console/content/content/userlist.social.0.10.json",
								"admin","admin",
								nameValuePairs);

						logger.debug(list);

						JSONArray jsonArray = new JSONObject(list).getJSONArray("items");
						if (jsonArray.length()==1) {

							JSONObject jsonObject = jsonArray.getJSONObject(0);
							String jsonElement = jsonObject.getString("type");

							if (jsonElement!=null && jsonElement.equals("user")) {

								// Always generating a page view event
								if (Math.random() < 0.90) doAnalytics(analytics, "event1", referer, resourceID, resourceType);

								// Sometimes generating a video view event
								if (Math.random() < 0.75 && resourceType.equals("video/mp4")) doAnalytics(analytics, "event2", referer, resourceID, resourceType);
									
								// Posting ratings and comments
								if (Math.random() < 0.50) doRatings(hostname, altport, key, resourceRatingsEndpoint, referer, resourceID, resourceType, analytics);
								if (Math.random() < 0.35) doComments(hostname, altport, key, resourceCommentsEndpoint, referer, resourceID, resourceType, analytics);

							} else {

								logger.debug("Enrollee is a group :" + key);
								List<NameValuePair>  groupNameValuePairs = new ArrayList<NameValuePair>();
								groupNameValuePairs.add(new BasicNameValuePair("groupId", key));
								groupNameValuePairs.add(new BasicNameValuePair("includeSubGroups", "true"));
								String memberList = Loader.doGet(hostname, altport,
										"/content/community-components/en/communitygroupmemberlist/jcr:content/content/communitygroupmember.social.0.100.json",
										"admin","admin",
										groupNameValuePairs);

								JSONArray memberJsonArray = new JSONObject(memberList).getJSONArray("items");
								for (int j=0; j<memberJsonArray.length();j++) {
									JSONObject memberJsonObject = memberJsonArray.getJSONObject(j);
									String email = memberJsonObject.getString("authorizableId");
									logger.debug("New group member for decoration: " + email);
									if (email!=null) {

										// Always generating a page view event
										if (Math.random() < 0.90) doAnalytics(analytics, "event1", referer, resourceID, "video/mp4");

										// Sometimes generating a video view event
										if (Math.random() < 0.75 && resourceType.equals("video/mp4")) doAnalytics(analytics, "event2", referer, resourceID, resourceType);
										
										if (Math.random() < 0.50) doRatings(hostname, altport, email, resourceRatingsEndpoint, referer, resourceID, resourceType, analytics);
										if (Math.random() < 0.35) doComments(hostname, altport, email, resourceCommentsEndpoint, referer, resourceID, resourceType, analytics);
									}

								} // For each group member

							} // If user or group

						} // If there's a principal name

					} // For each enrollee

					break; // only one possible deltaList attribute for resource and learning paths

				}

			}

		} catch (Exception e) {

			logger.error(e.getMessage());

		}

	}

	// This methods POSTS an analytics event
	private static void doAnalytics(String analytics, String event, String pageURL, String resourcePath, String resourceType) {

		if (analytics!=null && pageURL!=null && resourcePath!=null && resourceType!=null && event!=null) {
			
			URLConnection urlConn = null;
			DataOutputStream printout = null;
			BufferedReader input = null;
			String tmp = null;
			try {

				URL pageurl = new URL( pageURL);
				StringBuffer sb = new StringBuffer("<?xml version=1.0 encoding=UTF-8?><request><sc_xml_ver>1.0</sc_xml_ver>");
				sb.append("<events>" + event + "</events>");
				sb.append("<pageURL>" + pageURL + "</pageURL>");
				sb.append("<pageName>" + pageurl.getPath().substring(1,pageurl.getPath().indexOf(".")).replaceAll("/",":") + "</pageName>");
				sb.append("<evar1>" + resourcePath + "</evar1>");
				sb.append("<evar2>" + resourceType + "</evar2>");
				sb.append("<visitorID>demomachine</visitorID>");
				sb.append("<reportSuiteID>" + analytics.substring(0,analytics.indexOf(".")) + "</reportSuiteID>");
				sb.append("</request>");

				logger.debug("New Analytics Event: " + sb.toString());

				URL sitecaturl = new URL( "http://" + analytics );

				urlConn = sitecaturl.openConnection();
				urlConn.setDoInput( true );
				urlConn.setDoOutput( true );
				urlConn.setUseCaches( false );
				urlConn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );

				printout = new DataOutputStream(urlConn.getOutputStream());

				printout.writeBytes( sb.toString() );
				printout.flush();
				printout.close();

				input = new BufferedReader( new InputStreamReader( urlConn.getInputStream( ) ) );

				while( null != ( ( tmp = input.readLine() ) ) )
				{
					logger.debug(tmp);
				}
				printout.close();
				input.close();
				
			} catch (Exception ex) {

				logger.error(ex.getMessage());

			}		

		}
		
	}

	// This methods POSTS a rating and comments
	private static void doRatings(String hostname, String altport, String key, String resourceRatingsEndpoint, String referer, String resourceID, String resourceType, String analytics) {

		try {

			// Posting a Rating for this resource
			List<NameValuePair> ratingNameValuePairs = new ArrayList<NameValuePair>();
			ratingNameValuePairs.add(new BasicNameValuePair(":operation", "social:postTallyResponse"));
			ratingNameValuePairs.add(new BasicNameValuePair("tallyType", "Rating"));
			int randomRating = (int) Math.ceil(Math.random()*5);
			logger.debug("Randomly Generated Rating: " + randomRating);
			logger.debug("Referer for Rating: " + referer);
			ratingNameValuePairs.add(new BasicNameValuePair("response", String.valueOf(randomRating)));
			doPost(hostname, altport,
					resourceRatingsEndpoint + ".social.json",
					key, "password",
					new UrlEncodedFormEntity(ratingNameValuePairs),
					null,
					referer);

			doAnalytics(analytics, "event4", referer, resourceID, resourceType);

		} catch(Exception e) {

			logger.error(e.getMessage());

		}

	}

	// This methods POSTS a rating and comments
	private static void doComments(String hostname, String altport, String key, String resourceCommentsEndpoint, String referer, String resourceID, String resourceType, String analytics) {

		try {

			// Posting a Comment for this resource
			int randomComment = (int) Math.ceil(Math.random()*5);
			List<NameValuePair> commentNameValuePairs = new ArrayList<NameValuePair>();
			commentNameValuePairs.add(new BasicNameValuePair(":operation", "social:createComment"));
			commentNameValuePairs.add(new BasicNameValuePair("message", comments[randomComment-1]));
			commentNameValuePairs.add(new BasicNameValuePair("id", "nobot"));
			logger.debug("Referer for Commenting: " + referer);
			doPost(hostname, altport,
					resourceCommentsEndpoint,
					key, "password",
					new UrlEncodedFormEntity(commentNameValuePairs),
					null,
					referer);

			doAnalytics(analytics, "event3", referer, resourceID, resourceType);

		} catch(Exception e) {

			logger.error(e.getMessage());

		}

	}

	// This method POSTs a request to the server, returning the location JSON attribute, when available
	private static String doPost(String hostname, String port, String url, String user, String password, HttpEntity entity, String lookup) {

		return doPost(hostname, port, url, user, password, entity, lookup, null);
		
	}
	
	private static String doPost(String hostname, String port, String url, String user, String password, HttpEntity entity, String lookup, String referer) {

		String jsonElement = null;

		try {

			HttpHost target = new HttpHost(hostname, Integer.parseInt(port), "http");
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(user, password));
			CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider).build();

			try {

				// Adding the Basic Authentication data to the context for this command
				AuthCache authCache = new BasicAuthCache();
				BasicScheme basicAuth = new BasicScheme();
				authCache.put(target, basicAuth);
				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				// Composing the root URL for all subsequent requests
				String postUrl = "http://" + hostname + ":" + port + url;
				logger.debug("Posting request as " + user + " with password " + password  + " to " + postUrl);

				// Preparing a standard POST HTTP command
				HttpPost request = new HttpPost(postUrl);
				request.setEntity(entity);
				if (!entity.getContentType().toString().contains("multipart")) {
					request.addHeader("content-type", "application/x-www-form-urlencoded");
				}
				request.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
				request.addHeader("Origin", postUrl);
				if (referer!=null) {
					logger.debug("Referer header added to request: " + referer);
					request.addHeader("Referer", referer);
				}

				// Sending the HTTP POST command
				CloseableHttpResponse response = httpClient.execute(target, request, localContext);
				try {            
					String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");   
					logger.debug("Got POST response:" + responseString); 
					if (lookup!=null) {
						logger.debug("JSON lookup value: " + lookup); 
						int separatorIndex = lookup.indexOf("/");
						if (separatorIndex>0) {
						
							// Grabbing element in a nested element
							Object object = new JSONObject(responseString).get(lookup.substring(0,separatorIndex));
							if (object!=null) {

								if (object instanceof JSONArray) {
									
									logger.debug("JSON object is a JSONArray");
									JSONArray jsonArray = (JSONArray) object;
									if (jsonArray.length()==1) {
										JSONObject jsonObject = jsonArray.getJSONObject(0);
										jsonElement = jsonObject.getString(lookup.substring(1 + separatorIndex));
										logger.debug("JSON value (jsonArray) returned is " + jsonElement);
									}

								} else if (object instanceof JSONObject) {

									logger.debug("JSON object is a JSONObject");
									JSONObject jsonobject = (JSONObject) object; 
									jsonElement = jsonobject.getString(lookup.substring(1 + separatorIndex));	
									logger.debug("JSON value (jsonObject) returned is " + jsonElement);
									
								}
							}

						} else {
							// Grabbing element at the top of the JSON response
							jsonElement = new JSONObject(responseString).getString(lookup);
							logger.debug("JSON (top) value returned is " + jsonElement);

						}
					}


				} catch (Exception ex) {
					logger.error(ex.getMessage());
				} finally {
					response.close();
				}

			} catch (Exception ex) {
				logger.error(ex.getMessage());				
			} finally {
				httpClient.close();
			}

		} catch (IOException e) {
			logger.error(e.getMessage());				
		}

		return jsonElement;

	}

	// This method DELETES a request to the server
	private static void doDelete(String hostname, String port, String url, String user, String password) {

		try {

			HttpHost target = new HttpHost(hostname, Integer.parseInt(port), "http");
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(user, password));
			CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider).build();

			try {

				// Adding the Basic Authentication data to the context for this command
				AuthCache authCache = new BasicAuthCache();
				BasicScheme basicAuth = new BasicScheme();
				authCache.put(target, basicAuth);
				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				// Composing the root URL for all subsequent requests
				String postUrl = "http://" + hostname + ":" + port + url;
				logger.debug("Deleting request as " + user + " with password " + password  + " to " + postUrl);
				HttpDelete request = new HttpDelete(postUrl);
				httpClient.execute(target, request, localContext);

			} catch (Exception ex) {
				logger.error(ex.getMessage());				
			} finally {
				httpClient.close();
			}

		} catch (IOException e) {
			logger.error(e.getMessage());				
		}

	}

	// This method WAITs for a group to be present on a server
	private static void doWait(String hostname, String port, String user, String password, String group) {

		if (group==null || group.length()==0) {
			logger.error("Group name was not provided - not waiting for group to be available");
			return;
		}
		
		if (hostname!=null && port!=null && password!=null && user!=null && (group!=null && group.length()>0)) {

			int retries = 0;

			// Retrieving the list of groups for the newly created site, using alternate port (publish in general)
			List<NameValuePair>  nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("filter", "[{\"operation\":\"like\",\"rep:principalName\":\"" + group + "\"}]"));
			nameValuePairs.add(new BasicNameValuePair("type", "groups"));
			while (retries++ < MAXRETRIES) {

				String groupList = Loader.doGet(hostname, port,
						"/libs/social/console/content/content/userlist.social.0.10.json",
						user, password,
						nameValuePairs);

				logger.debug(groupList);

				if (groupList.indexOf(group)>0) {

					logger.debug("Group was found on " + port);
					break;

				} else {

					doSleep(2000,"Group " + group + " not found yet");

				}

			}
			
			if (retries==MAXRETRIES) {
				logger.error("Group " + group +" was never found as expected");
			}
			
		}


	}

	// This method runs a QUERY against an AEM instance
	private static String doQuery(String hostname, String port, String adminPassword, String path, String type) {

		String query = null;

		if (port!=null && hostname!=null && path!=null && type!=null) {

			List<NameValuePair>  nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("path", path));
			nameValuePairs.add(new BasicNameValuePair("type", type));
			String nodeList = Loader.doGet(hostname, port,
					"/bin/querybuilder.json",
					"admin", adminPassword,
					nameValuePairs);

			logger.debug(nodeList);
			query = nodeList;
		}

		return query;
	}

	// This method WAITS for a node to be available
	private static void doWaitPath(String hostname, String port, String adminPassword, String path, String type) {

		int retries = 0;
		while (retries++ < MAXRETRIES) {

			String nodeList = doQuery(hostname, port, adminPassword, path, type);
			try {            
				JSONObject nodeListJson = new JSONObject(nodeList);
				int results = nodeListJson.getInt("results");
				if (results>0) {

					logger.debug("Node was found for: " + path);
					break;

				} else {

					doSleep(2000,"Node not found yet, repeating " + retries);

				}

			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}

		}

	}

	// This method GETs a request to the server, returning the location JSON attribute, when available
	private static String doGet(String hostname, String port, String url, String user, String password, List<NameValuePair> params) {

		String rawResponse = null;

		try {

			HttpHost target = new HttpHost(hostname, Integer.parseInt(port), "http");
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(user, password));
			CloseableHttpClient httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider).build();

			try {

				// Adding the Basic Authentication data to the context for this command
				AuthCache authCache = new BasicAuthCache();
				BasicScheme basicAuth = new BasicScheme();
				authCache.put(target, basicAuth);
				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);

				// Composing the root URL for all subsequent requests
				URIBuilder uribuilder = new URIBuilder();
				uribuilder.setScheme("http")
				.setHost(hostname)
				.setPort(Integer.parseInt(port))
				.setPath(url);

				// Adding the params
				if (params!=null) for (NameValuePair nvp : params) {
					uribuilder.setParameter(nvp.getName(), nvp.getValue());
				}

				URI uri = uribuilder.build();
				logger.debug("URI built as " + uri.toString());
				HttpGet httpget = new HttpGet(uri);
				CloseableHttpResponse response = httpClient.execute(httpget, localContext);
				try {     
					rawResponse = EntityUtils.toString(response.getEntity(), "UTF-8");   
				} catch (Exception ex) {
					logger.error(ex.getMessage());
				} finally {
					response.close();
				}

			} catch (Exception ex) {
				logger.error(ex.getMessage());
			} finally {
				httpClient.close();
			}

		} catch (IOException e) {

			e.printStackTrace();
		}

		return rawResponse;

	}

	// This method builds a list of NVP for a subsequent Sling post
	private static List<NameValuePair> buildNVP(CSVRecord record, int start) {

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("_charset_", "UTF-8"));

		for (int i=start;i<record.size()-1;i=i+2) {

			if (record.get(i)!=null && record.get(i+1)!=null && record.get(i).length()>0) {

				// We have a non String hint to pass to the POST servlet
				String name = record.get(i);
				String value = record.get(i+1);
				if (value.equals("TRUE")) { value = "true"; }
				if (value.equals("FALSE")) { value = "false"; }		

				int hint = name.indexOf("@");
				if (hint>0) {
					logger.debug(name.substring(0,hint) + "@TypeHint:" + name.substring(1+hint));
					nameValuePairs.add(new BasicNameValuePair(name.substring(0,hint) + "@TypeHint", name.substring(1+hint)));					            		
					name = name.substring(0,hint);
				} else {
					nameValuePairs.add(new BasicNameValuePair(name + "@TypeHint", "String"));					            							            		
				}

				// We have multiple values to pass to the POST servlet, e.g. for a String[]
				int multiple = value.indexOf("|");
				if (multiple>0) {
					List<String> values = Arrays.asList(value.split("\\|", -1));
					for (String currentValue : values) {
						nameValuePairs.add(new BasicNameValuePair(name, currentValue));	
						logger.debug(name + " " + currentValue);
					}
				} else {					            		
					nameValuePairs.add(new BasicNameValuePair(name, value));					            							            		
				}

				logger.debug("Setting property "+ name + " with value " + value );

			}

		}	

		return nameValuePairs;

	}

}
