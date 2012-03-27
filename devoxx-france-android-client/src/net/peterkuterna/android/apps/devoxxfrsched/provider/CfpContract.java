/*
 * Copyright 2011 Google Inc.
 * Copyright 2011 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.devoxxfrsched.provider;

import java.util.List;

import net.peterkuterna.android.apps.devoxxfrsched.util.ParserUtils;
import android.app.SearchManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;

/**
 * Contract class for interacting with {@link CfpProvider}. Unless otherwise
 * noted, all time-based fields are milliseconds since epoch and can be compared
 * against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class CfpContract {

	/**
	 * Special value for {@link SyncColumns#UPDATED} indicating that an entry
	 * has never been updated, or doesn't exist yet.
	 */
	public static final long UPDATED_NEVER = -2;

	/**
	 * Special value for {@link SyncColumns#UPDATED} indicating that the last
	 * update time is unknown, usually when inserted from a local file source.
	 */
	public static final long UPDATED_UNKNOWN = -1;

	public static final long MARK_AS_DELETED = 1;
	public static final long NOT_DELETED = 0;

	public interface SyncColumns {
		// /** Last time this entry was updated or synchronized. */
		String UPDATED = "updated";
		/** Mark a record as deleted */
		String DELETED = "deleted";
	}

	interface BlocksColumns {
		/** Unique string identifying this block of time. */
		String BLOCK_ID = "block_id";
		/** Title describing this block of time. */
		String BLOCK_TITLE = "block_title";
		/** Time when this block starts. */
		String BLOCK_START = "block_start";
		/** Time when this block ends. */
		String BLOCK_END = "block_end";
		/** Kind describing this block. */
		String BLOCK_KIND = "block_kind";
		/** Type describing this block. */
		String BLOCK_TYPE = "block_type";
		/** Code describing this block. */
		String BLOCK_CODE = "block_code";
	}

	interface TracksColumns {
		/** Unique string identifying this track. */
		String TRACK_ID = "track_id";
		/** Name describing this track. */
		String TRACK_NAME = "track_name";
		/** Color used to identify this track, in {@link Color#argb} format. */
		String TRACK_COLOR = "track_color";
		/** Body of text explaining this track in detail. */
		String TRACK_DESCRIPTION = "track_description";
		/** Hashtag for the track. */
		String TRACK_HASHTAG = "track_hashtag";
	}

	interface RoomsColumns {
		/** Unique string identifying this room. */
		String ROOM_ID = "room_id";
		/** Name describing this room. */
		String ROOM_NAME = "room_name";
		/** Capacity of the room. */
		String ROOM_CAPACITY = "room_capacity";
		/** Level of the room. */
		String ROOM_LEVEL = "room_level";
	}

	interface SessionsColumns {
		/** Unique string identifying this session. */
		String SESSION_ID = "session_id";
		/** Difficulty level of the session. */
		String SESSION_EXPERIENCE = "session_experience";
		/** Title describing this track. */
		String SESSION_TITLE = "session_title";
		/** Body of text explaining this session in detail. */
		String SESSION_SUMMARY = "session_summary";
		/** Kewords/tags for this session. */
		String SESSION_KEYWORDS = "session_keywords";
		/** Full URL to session online. */
		String SESSION_URL = "session_url";
		/** User-specific flag indicating starred status. */
		String SESSION_STARRED = "session_starred";
		/** Flag to indicate a new session. */
		String SESSION_NEW = "session_new";
		/** Timestamp when new session was introduced. */
		String SESSION_NEW_TIMESTAMP = "session_new_timestamp";
		/** AppEngine operation pending */
		String SESSION_OPERATION_PENDING = "session_operation_pending";
	}

	interface TagsColumns {
		/** Unique string identifying this tag. */
		String TAG_ID = "tag_id";
		/** Tag name. */
		String TAG_NAME = "tag_name";
	}

	interface SpeakersColumns {
		/** Unique string identifying this speaker. */
		String SPEAKER_ID = "speaker_id";
		/** First name of this speaker. */
		String SPEAKER_FIRSTNAME = "speaker_firstname";
		/** Last name of this speaker. */
		String SPEAKER_LASTNAME = "speaker_lastname";
		/** Profile photo of this speaker. */
		String SPEAKER_IMAGE_URL = "speaker_image_url";
		/** Company this speaker works for. */
		String SPEAKER_COMPANY = "speaker_company";
		/** Body of text describing this speaker in detail. */
		String SPEAKER_BIO = "speaker_bio";
	}

	interface SessionTypesColumns {
		/** Unique string identifying session type. */
		String SESSION_TYPE_ID = "session_type_id";
		/** Name of session type. */
		String SESSION_TYPE_NAME = "session_type_name";
		/** Description of the session type. */
		String SESSION_TYPE_DESCRIPTION = "session_type_description";
	}

	interface TweetsColumns {
		/** Unique string identifying this tweet. */
		String TWEET_ID = "tweet_id";
		/** Text of the tweet. */
		String TWEET_TEXT = "tweet_text";
		/** Creation time of the tweet. */
		String TWEET_CREATED_AT = "tweet_created_at";
		/** User that created the tweet. */
		String TWEET_USER = "tweet_user";
		/** URL to the image of the user. */
		String TWEET_IMAGE_URI = "tweet_image_uri";
		/** Type of tweet result. */
		String TWEET_RESULT_TYPE = "tweet_result_type";
	}

	interface NewsColumns {
		/** Unique string identifying this news. */
		String NEWS_ID = "news_id";
		/** Text of the tweet. */
		String NEWS_TEXT = "news_text";
		/** Creation time of the tweet. */
		String NEWS_CREATED_AT = "news_created_at";
		/** Type of tweet result. */
		String NEWS_RESULT_TYPE = "news_result_type";
		/** Mark as new or not */
		String NEWS_NEW = "news_new";
	}

	public static final String CONTENT_AUTHORITY = "net.peterkuterna.android.apps.devoxxfrsched";

	private static final Uri BASE_CONTENT_URI = Uri.parse("content://"
			+ CONTENT_AUTHORITY);

	private static final String PATH_BLOCKS = "blocks";
	private static final String PATH_PARALLEL = "parallel";
	private static final String PATH_AT = "at";
	private static final String PATH_BETWEEN = "between";
	private static final String PATH_TRACKS = "tracks";
	private static final String PATH_ROOMS = "rooms";
	private static final String PATH_SESSION = "session";
	private static final String PATH_SESSIONS = "sessions";
	private static final String PATH_STARRED = "starred";
	private static final String PATH_SPEAKERS = "speakers";
	private static final String PATH_SESSION_TYPES = "sessiontypes";
	private static final String PATH_SEARCH = "search";
	private static final String PATH_TWEETS = "tweets";
	private static final String PATH_NEWS = "news";
	private static final String PATH_TAGS = "tags";
	private static final String PATH_NEW = "new";
	private static final String PATH_PARLEYS = "parleys";
	private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";

	/**
	 * Blocks are generic timeslots that {@link Sessions} and other related
	 * events fall into.
	 */
	public static class Blocks implements BlocksColumns, SyncColumns,
			BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_BLOCKS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.block";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.block";

		/** Count of {@link Sessions} inside given block. */
		public static final String SESSIONS_COUNT = "sessions_count";

		/**
		 * Flag indicating that at least one {@link Sessions#SESSION_ID} inside
		 * this block has {@link Sessions#SESSION_STARRED} set.
		 */
		public static final String CONTAINS_STARRED = "contains_starred";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START
				+ " ASC, " + BlocksColumns.BLOCK_END + " ASC";

		/** Build {@link Uri} for requested {@link #BLOCK_ID}. */
		public static Uri buildBlockUri(String blockId) {
			return CONTENT_URI.buildUpon().appendPath(blockId).build();
		}

		/**
		 * Build {@link Uri} that returns the single {@link Sessions} associated
		 * with the requested {@link #BLOCK_ID}.
		 */
		public static Uri buildSessionUri(String blockId) {
			return CONTENT_URI.buildUpon().appendPath(blockId)
					.appendPath(PATH_SESSION).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #BLOCK_ID}.
		 */
		public static Uri buildSessionsUri(String blockId) {
			return CONTENT_URI.buildUpon().appendPath(blockId)
					.appendPath(PATH_SESSIONS).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Blocks} that occur
		 * between the requested time boundaries.
		 */
		public static Uri buildBlocksBetweenDirUri(long startTime, long endTime) {
			return CONTENT_URI.buildUpon().appendPath(PATH_BETWEEN)
					.appendPath(String.valueOf(startTime))
					.appendPath(String.valueOf(endTime)).build();
		}

		/** Read {@link #BLOCK_ID} from {@link Blocks} {@link Uri}. */
		public static String getBlockId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #BLOCK_ID} that will always match the requested
		 * {@link Blocks} details.
		 */
		public static String generateBlockId(String kind, long startTime,
				long endTime) {
			startTime /= DateUtils.SECOND_IN_MILLIS;
			endTime /= DateUtils.SECOND_IN_MILLIS;
			return ParserUtils.sanitizeId(kind + "-" + startTime + "-"
					+ endTime);
		}
	}

	/**
	 * Tracks are overall categories for {@link Sessions}.
	 */
	public static class Tracks implements TracksColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_TRACKS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.track";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.track";

		/** Count of {@link Sessions} inside given track. */
		public static final String SESSIONS_COUNT = "sessions_count";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = TracksColumns.TRACK_NAME
				+ " ASC";

		/** "All tracks" ID. */
		public static final String ALL_TRACK_ID = "all";

		/** Build {@link Uri} for requested {@link #TRACK_ID}. */
		public static Uri buildTrackUri(String trackId) {
			return CONTENT_URI.buildUpon().appendPath(trackId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #TRACK_ID}.
		 */
		public static Uri buildSessionsUri(String trackId) {
			return CONTENT_URI.buildUpon().appendPath(trackId)
					.appendPath(PATH_SESSIONS).build();
		}

		/** Read {@link #TRACK_ID} from {@link Tracks} {@link Uri}. */
		public static String getTrackId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #TRACK_ID} that will always match the requested
		 * {@link Tracks} details.
		 */
		public static String generateTrackId(String trackName) {
			return ParserUtils.sanitizeId(trackName);
		}
	}

	/**
	 * Rooms are physical locations at the conference venue.
	 */
	public static class Rooms implements RoomsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_ROOMS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.room";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.room";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = RoomsColumns.ROOM_NAME
				+ " COLLATE NOCASE ASC";

		/** Build {@link Uri} for requested {@link #ROOM_ID}. */
		public static Uri buildRoomUri(String roomId) {
			return CONTENT_URI.buildUpon().appendPath(roomId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #ROOM_ID}.
		 */
		public static Uri buildSessionsDirUri(String roomId) {
			return CONTENT_URI.buildUpon().appendPath(roomId)
					.appendPath(PATH_SESSIONS).build();
		}

		/** Read {@link #ROOM_ID} from {@link Rooms} {@link Uri}. */
		public static String getRoomId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #ROOM_ID} that will always match the requested
		 * {@link Rooms} details.
		 */
		public static String generateRoomId(String room) {
			return ParserUtils.sanitizeId(room);
		}
	}

	/**
	 * Each session is a block of time that has a {@link Tracks}, a
	 * {@link Rooms}, and zero or more {@link Speakers}.
	 */
	public static class Sessions implements SessionsColumns, BlocksColumns,
			RoomsColumns, TracksColumns, SessionTypesColumns, SyncColumns,
			BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_SESSIONS).build();
		public static final Uri CONTENT_STARRED_URI = CONTENT_URI.buildUpon()
				.appendPath(PATH_STARRED).build();
		public static final Uri CONTENT_NEW_URI = CONTENT_URI.buildUpon()
				.appendPath(PATH_NEW).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.session";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.session";

		public static final String BLOCK_ID = "block_id";
		public static final String ROOM_ID = "room_id";
		public static final String TRACK_ID = "track_id";
		public static final String SESSION_TYPE_ID = "session_type_id";

		public static final String STARRED_IN_BLOCK_COUNT = "starred_in_block_count";

		public static final String SEARCH_SNIPPET = "search_snippet";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START
				+ " ASC," + SessionsColumns.SESSION_TITLE
				+ " COLLATE NOCASE ASC";

		/** Build {@link Uri} for requested {@link #SESSION_ID}. */
		public static Uri buildSessionUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(sessionId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Speakers} associated
		 * with the requested {@link #SESSION_ID}.
		 */
		public static Uri buildSpeakersDirUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(sessionId)
					.appendPath(PATH_SPEAKERS).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Tags} associated with
		 * the requested {@link #SESSION_ID}.
		 */
		public static Uri buildTagsDirUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(sessionId)
					.appendPath(PATH_TAGS).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Tracks} associated with
		 * the requested {@link #SESSION_ID}.
		 */
		public static Uri buildTracksDirUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(sessionId)
					.appendPath(PATH_TRACKS).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #SESSION_ID} and are running in parallel.
		 */
		public static Uri buildSessionsParallelDirUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(PATH_PARALLEL)
					.appendPath(sessionId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link ParleysPresentations}
		 * associated with the requested {@link #SESSION_ID}.
		 */
		public static Uri buildParleysDirUri(String sessionId) {
			return CONTENT_URI.buildUpon().appendPath(sessionId)
					.appendPath(PATH_PARLEYS).build();
		}

		public static Uri buildSessionsAtDirUri(long time) {
			return CONTENT_URI.buildUpon().appendPath(PATH_AT)
					.appendPath(String.valueOf(time)).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} that occur
		 * between the requested time boundaries.
		 */
		public static Uri buildSessionsBetweenDirUri(long startTime,
				long endTime) {
			return CONTENT_URI.buildUpon().appendPath(PATH_BETWEEN)
					.appendPath(String.valueOf(startTime))
					.appendPath(String.valueOf(endTime)).build();
		}

		public static Uri buildSearchUri(String query) {
			return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH)
					.appendPath(query).build();
		}

		public static boolean isSearchUri(Uri uri) {
			if (uri != null) {
				List<String> pathSegments = uri.getPathSegments();
				return pathSegments.size() >= 2
						&& PATH_SEARCH.equals(pathSegments.get(1));
			}
			return false;
		}

		public static String getSearchQuery(Uri uri) {
			return uri.getPathSegments().get(2);
		}

		public static boolean isNewSessionsUri(Uri uri) {
			if (uri != null) {
				List<String> pathSegments = uri.getPathSegments();
				return pathSegments.size() == 2
						&& PATH_NEW.equals(pathSegments.get(1));
			}
			return false;
		}

		/** Read {@link #SESSION_ID} from {@link Sessions} {@link Uri}. */
		public static String getSessionId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #SESSION_ID} that will always match the requested
		 * {@link Sessions} details.
		 */
		public static String generateSessionId(int sessionId) {
			return String.valueOf(sessionId);
		}
	}

	/**
	 * Tags.
	 */
	public static class Tags implements TagsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_TAGS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxx.tag";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxx.tag";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = TagsColumns.TAG_NAME + " ASC";

		/** Build {@link Uri} for requested {@link #TAG_ID}. */
		public static Uri buildTagUri(String tagId) {
			return CONTENT_URI.buildUpon().appendPath(tagId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #TAG_ID}.
		 */
		public static Uri buildSessionsDirUri(String tagId) {
			return CONTENT_URI.buildUpon().appendPath(tagId)
					.appendPath(PATH_SESSIONS).build();
		}

		/** Read {@link #TAG_ID} from {@link Tags} {@link Uri}. */
		public static String getTagId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #TAG_ID} that will always match the requested
		 * {@link Tags} details.
		 */
		public static String generateTagId(String tagName) {
			return ParserUtils.sanitizeId(tagName);
		}

	}

	/**
	 * Speakers are individual people that lead {@link Sessions}.
	 */
	public static class Speakers implements SpeakersColumns, SyncColumns,
			BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_SPEAKERS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.speaker";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.speaker";

		public static final String SEARCH_SNIPPET = "search_snippet";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = SpeakersColumns.SPEAKER_FIRSTNAME
				+ " COLLATE NOCASE ASC, "
				+ SpeakersColumns.SPEAKER_LASTNAME
				+ " COLLATE NOCASE ASC";

		/** Build {@link Uri} for requested {@link #SPEAKER_ID}. */
		public static Uri buildSpeakerUri(String speakerId) {
			return CONTENT_URI.buildUpon().appendPath(speakerId).build();
		}

		public static Uri buildSearchUri(String query) {
			return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH)
					.appendPath(query).build();
		}

		public static boolean isSearchUri(Uri uri) {
			List<String> pathSegments = uri.getPathSegments();
			return pathSegments.size() >= 2
					&& PATH_SEARCH.equals(pathSegments.get(1));
		}

		public static String getSearchQuery(Uri uri) {
			return uri.getPathSegments().get(2);
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #SPEAKER_ID}.
		 */
		public static Uri buildSessionsDirUri(String speakerId) {
			return CONTENT_URI.buildUpon().appendPath(speakerId)
					.appendPath(PATH_SESSIONS).build();
		}

		/** Read {@link #SPEAKER_ID} from {@link Speakers} {@link Uri}. */
		public static String getSpeakerId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #SPEAKER_ID} that will always match the requested
		 * {@link Speakers} details.
		 */
		public static String generateSpeakerId(int speakerId) {
			return String.valueOf(speakerId);
		}
	}

	/**
	 * Describes the different session types.
	 */
	public static class SessionTypes implements SessionTypesColumns,
			BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_SESSION_TYPES).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.sessiontype";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.sessiontype";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = SessionTypesColumns.SESSION_TYPE_NAME
				+ " COLLATE NOCASE ASC";

		/** Build {@link Uri} for requested {@link #SESSION_TYPE_ID}. */
		public static Uri buildSessionTypeUri(String sessionTypeId) {
			return CONTENT_URI.buildUpon().appendPath(sessionTypeId).build();
		}

		/** Read {@link #SESSION_TYPE_ID} from {@link Vendors} {@link Uri}. */
		public static String getSessionTypeId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #SESSION_TYPE_ID} that will always match the
		 * requested {@link SessionTypes} details.
		 */
		public static String generateSessionTypeId(String sessionTypeName) {
			return ParserUtils.sanitizeId(sessionTypeName);
		}
	}

	/**
	 * Tweets.
	 */
	public static class Tweets implements TweetsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_TWEETS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.tweet";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.tweet";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = TweetsColumns.TWEET_RESULT_TYPE
				+ " ASC, " + TweetsColumns.TWEET_CREATED_AT + " DESC";

		/** Build {@link Uri} for requested {@link #TWEET_ID}. */
		public static Uri buildTweetUri(String tweetId) {
			return CONTENT_URI.buildUpon().appendPath(tweetId).build();
		}

		/** Read {@link #TWEET_ID} from {@link Tweets} {@link Uri}. */
		public static String getTweetId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

	}

	/**
	 * News.
	 */
	public static class News implements NewsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_NEWS).build();
		public static final Uri CONTENT_NEW_URI = CONTENT_URI.buildUpon()
				.appendPath(PATH_NEW).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.devoxxfrsched.news";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.devoxxfrsched.news";

		/** Default "ORDER BY" clause. */
		public static final String DEFAULT_SORT = NEWS_CREATED_AT + " DESC";

		/** Build {@link Uri} for requested {@link #NEWS_ID}. */
		public static Uri buildNewsUri(String newsId) {
			return CONTENT_URI.buildUpon().appendPath(newsId).build();
		}

		/** Read {@link #NEWS_ID} from {@link News} {@link Uri}. */
		public static String getNewsId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #NEWS_ID} that will always match the requested
		 * {@link News} details.
		 */
		public static String generateNewsId(String newsDate) {
			return ParserUtils.sanitizeId(newsDate);
		}
	}

	public static class SearchSuggest {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH_SEARCH_SUGGEST).build();

		public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
				+ " COLLATE NOCASE ASC";
	}

	private CfpContract() {
	}

}
