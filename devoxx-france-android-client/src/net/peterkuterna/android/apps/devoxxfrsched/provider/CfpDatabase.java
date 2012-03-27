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

import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.BlocksColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.NewsColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.RoomsColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SessionTypes;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SessionTypesColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SessionsColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SpeakersColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.SyncColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tags;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.TagsColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.TracksColumns;
import net.peterkuterna.android.apps.devoxxfrsched.provider.CfpContract.TweetsColumns;
import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class CfpDatabase extends SQLiteOpenHelper {

	private static final String TAG = "CfpDatabase";

	private static final String DATABASE_NAME = "schedule.db";

	private static final int VER_LAUNCH = 1;

	private static final int DATABASE_VERSION = VER_LAUNCH;

	public interface Tables {
		String BLOCKS = "blocks";
		String TRACKS = "tracks";
		String ROOMS = "rooms";
		String SESSIONS = "sessions";
		String SPEAKERS = "speakers";
		String TAGS = "tags";
		String SESSIONS_SPEAKERS = "sessions_speakers";
		String SESSIONS_TAGS = "sessions_tags";
		String SESSION_TYPES = "session_types";

		String SESSIONS_SEARCH = "sessions_search";
		String SPEAKERS_SEARCH = "speakers_search";
		String TWEETS = "tweets";
		String NEWS = "news";

		String SEARCH_SUGGEST = "search_suggest";

		// old tables
		String NOTES = "notes";
		String SYNC = "sync";
		String TYPES = "types";

		// joins
		String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS = "sessions "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
				+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

		String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
				+ "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

		String SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_speakers "
				+ "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SESSIONS_TAGS_JOIN_TAGS = "sessions_tags "
				+ "LEFT OUTER JOIN tags ON sessions_tags.tag_id=tags.tag_id";

		String SESSIONS_TAGS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_tags "
				+ "LEFT OUTER JOIN sessions ON sessions_tags.session_id=sessions.session_id "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SESSIONS_JOIN_TRACKS = "sessions "
				+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

		String SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_tracks "
				+ "LEFT OUTER JOIN sessions ON sessions_tracks.session_id=sessions.session_id "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_search "
				+ "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
				+ "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
				+ "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

		String SPEAKERS_SEARCH_JOIN_SPEAKERS = "speakers_search "
				+ "LEFT OUTER JOIN speakers ON speakers_search.speaker_id=speakers.speaker_id";
	}

	private interface Triggers {
		String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
		String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
		String SESSIONS_SEARCH_UPDATE = "sessions_search_update";

		String SPEAKERS_SEARCH_INSERT = "speakers_search_insert";
		String SPEAKERS_SEARCH_DELETE = "speakers_search_delete";
		String SPEAKERS_SEARCH_UPDATE = "speakers_search_update";
	}

	public interface SessionsSpeakers {
		String SESSION_ID = "session_id";
		String SPEAKER_ID = "speaker_id";
	}

	public interface SessionsTags {
		String SESSION_ID = "session_id";
		String TAG_ID = "tag_id";
	}

	public interface SessionsTracks {
		String SESSION_ID = "session_id";
		String TRACK_ID = "track_id";
	}

	interface SessionsSearchColumns {
		String SESSION_ID = "session_id";
		String BODY = "body";
	}

	interface SpeakersSearchColumns {
		String SPEAKER_ID = "speaker_id";
		String BODY = "body";
	}

	/** Fully-qualified field names. */
	private interface Qualified {
		String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "."
				+ SessionsSearchColumns.SESSION_ID;
		String SPEAKERS_SEARCH_SPEAKER_ID = Tables.SPEAKERS_SEARCH + "."
				+ SpeakersSearchColumns.SPEAKER_ID;

		String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "("
				+ SessionsSearchColumns.SESSION_ID + ","
				+ SessionsSearchColumns.BODY + ")";
		String SPEAKERS_SEARCH = Tables.SPEAKERS_SEARCH + "("
				+ SpeakersSearchColumns.SPEAKER_ID + ","
				+ SpeakersSearchColumns.BODY + ")";
	}

	/** {@code REFERENCES} clauses. */
	private interface References {
		String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID
				+ ")";
		String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID
				+ ")";
		String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID
				+ ")";
		String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "("
				+ Sessions.SESSION_ID + ")";
		String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "("
				+ Speakers.SPEAKER_ID + ")";
		String TAG_ID = "REFERENCES " + Tables.TAGS + "(" + Tags.TAG_ID + ")";
		String SESSION_TYPE_ID = "REFERENCES " + Tables.SESSION_TYPES + "("
				+ SessionTypes.SESSION_TYPE_ID + ")";
	}

	private interface Subquery {
		/**
		 * Subquery used to build the {@link SessionsSearchColumns#BODY} string
		 * used for indexing {@link Sessions} content.
		 */
		String SESSIONS_BODY = "(new." + Sessions.SESSION_TITLE
				+ "||'; '||new." + Sessions.SESSION_SUMMARY + "||'; '||"
				+ "coalesce(new." + Sessions.SESSION_KEYWORDS + ", '')" + ")";

		/**
		 * Subquery used to build the {@link SpeakersSearchColumns#BODY} string
		 * used for indexing {@link Speakers} content.
		 */
		String SPEAKERS_BODY = "(new." + Speakers.SPEAKER_FIRSTNAME
				+ "||'; '||new." + Speakers.SPEAKER_LASTNAME + "||'; '||new."
				+ Speakers.SPEAKER_COMPANY + "||'; '||new."
				+ Speakers.SPEAKER_BIO + ")";
	}

	public CfpDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void cleanupLinkTables() {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
			db.execSQL("DELETE FROM " + Tables.SESSIONS_SPEAKERS + " WHERE "
					+ SessionsSpeakers.SESSION_ID + " NOT IN (SELECT "
					+ Sessions.SESSION_ID + " FROM " + Tables.SESSIONS + ")");
			db.execSQL("DELETE FROM " + Tables.SESSIONS_SPEAKERS + " WHERE "
					+ SessionsSpeakers.SPEAKER_ID + " NOT IN (SELECT "
					+ Speakers.SPEAKER_ID + " FROM " + Tables.SPEAKERS + ")");
			db.execSQL("DELETE FROM " + Tables.SPEAKERS + " WHERE "
					+ Speakers.SPEAKER_ID + " NOT IN (SELECT "
					+ SessionsSpeakers.SPEAKER_ID + " FROM "
					+ Tables.SESSIONS_SPEAKERS + ")");
			db.execSQL("DELETE FROM " + Tables.SESSIONS_TAGS + " WHERE "
					+ SessionsTags.SESSION_ID + " NOT IN (SELECT "
					+ Sessions.SESSION_ID + " FROM " + Tables.SESSIONS + ")");
			db.execSQL("DELETE FROM " + Tables.SESSIONS_TAGS + " WHERE "
					+ SessionsTags.TAG_ID + " NOT IN (SELECT " + Tags.TAG_ID
					+ " FROM " + Tables.TAGS + ")");
			db.execSQL("DELETE FROM " + Tables.TAGS + " WHERE " + Tags.TAG_ID
					+ " NOT IN (SELECT " + SessionsTags.TAG_ID + " FROM "
					+ Tables.SESSIONS_TAGS + ")");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			db.close();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + Tables.SESSIONS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
				+ " INTEGER NOT NULL," + SyncColumns.DELETED
				+ " INTEGER NOT NULL," + SessionsColumns.SESSION_ID
				+ " TEXT NOT NULL," + Sessions.BLOCK_ID + " TEXT "
				+ References.BLOCK_ID + "," + Sessions.ROOM_ID + " TEXT "
				+ References.ROOM_ID + "," + Sessions.TRACK_ID + " TEXT "
				+ References.TRACK_ID + "," + Sessions.SESSION_TYPE_ID
				+ " TEXT " + References.SESSION_TYPE_ID + ","
				+ SessionsColumns.SESSION_EXPERIENCE + " TEXT,"
				+ SessionsColumns.SESSION_TITLE + " TEXT,"
				+ SessionsColumns.SESSION_SUMMARY + " TEXT,"
				+ SessionsColumns.SESSION_KEYWORDS + " TEXT,"
				+ SessionsColumns.SESSION_URL + " TEXT,"
				+ SessionsColumns.SESSION_STARRED
				+ " INTEGER NOT NULL DEFAULT 0," + Sessions.SESSION_NEW
				+ " INTEGER NOT NULL DEFAULT 0,"
				+ Sessions.SESSION_NEW_TIMESTAMP
				+ " INTEGER NOT NULL DEFAULT 0,"
				+ Sessions.SESSION_OPERATION_PENDING
				+ " INTEGER NOT NULL DEFAULT 0," + "UNIQUE ("
				+ SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
				+ " INTEGER NOT NULL," + SyncColumns.DELETED
				+ " INTEGER NOT NULL," + SpeakersColumns.SPEAKER_ID
				+ " TEXT NOT NULL," + SpeakersColumns.SPEAKER_LASTNAME
				+ " TEXT," + SpeakersColumns.SPEAKER_FIRSTNAME + " TEXT,"
				+ SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
				+ SpeakersColumns.SPEAKER_COMPANY + " TEXT,"
				+ SpeakersColumns.SPEAKER_BIO + " TEXT," + "UNIQUE ("
				+ SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.TAGS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + TagsColumns.TAG_ID
				+ " TEXT NOT NULL," + TagsColumns.TAG_NAME + " TEXT NOT NULL,"
				+ "UNIQUE (" + TagsColumns.TAG_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SessionsSpeakers.SESSION_ID + " TEXT NOT NULL "
				+ References.SESSION_ID + "," + SessionsSpeakers.SPEAKER_ID
				+ " TEXT NOT NULL " + References.SPEAKER_ID + "," + "UNIQUE ("
				+ SessionsSpeakers.SESSION_ID + ","
				+ SessionsSpeakers.SPEAKER_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SESSIONS_TAGS + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SessionsTags.SESSION_ID + " TEXT NOT NULL "
				+ References.SESSION_ID + "," + SessionsTags.TAG_ID
				+ " TEXT NOT NULL " + References.TAG_ID + "," + "UNIQUE ("
				+ SessionsTags.SESSION_ID + "," + SessionsTags.TAG_ID
				+ ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.BLOCKS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
				+ " INTEGER NOT NULL," + SyncColumns.DELETED
				+ " INTEGER NOT NULL," + BlocksColumns.BLOCK_ID
				+ " TEXT NOT NULL," + BlocksColumns.BLOCK_TITLE
				+ " TEXT NOT NULL," + BlocksColumns.BLOCK_START
				+ " INTEGER NOT NULL," + BlocksColumns.BLOCK_END
				+ " INTEGER NOT NULL," + BlocksColumns.BLOCK_KIND + " TEXT,"
				+ BlocksColumns.BLOCK_TYPE + " TEXT,"
				+ BlocksColumns.BLOCK_CODE + " TEXT," + "UNIQUE ("
				+ BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.TRACKS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TracksColumns.TRACK_ID + " TEXT NOT NULL,"
				+ TracksColumns.TRACK_NAME + " TEXT,"
				+ TracksColumns.TRACK_COLOR + " INTEGER,"
				+ TracksColumns.TRACK_DESCRIPTION + " TEXT,"
				+ TracksColumns.TRACK_HASHTAG + " TEXT," + "UNIQUE ("
				+ TracksColumns.TRACK_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.ROOMS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + RoomsColumns.ROOM_ID
				+ " TEXT NOT NULL," + RoomsColumns.ROOM_NAME + " TEXT,"
				+ RoomsColumns.ROOM_CAPACITY + " INTEGER,"
				+ RoomsColumns.ROOM_LEVEL + " INTEGER," + "UNIQUE ("
				+ RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SESSION_TYPES + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SessionTypesColumns.SESSION_TYPE_ID + " TEXT NOT NULL,"
				+ SessionTypesColumns.SESSION_TYPE_NAME + " TEXT,"
				+ SessionTypesColumns.SESSION_TYPE_DESCRIPTION + " TEXT,"
				+ "UNIQUE (" + SessionTypesColumns.SESSION_TYPE_ID
				+ ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.TWEETS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ TweetsColumns.TWEET_ID + " INTEGER NOT NULL,"
				+ TweetsColumns.TWEET_TEXT + " TEXT NOT NULL,"
				+ TweetsColumns.TWEET_USER + " TEXT NOT NULL,"
				+ TweetsColumns.TWEET_CREATED_AT + " INTEGER NOT NULL,"
				+ TweetsColumns.TWEET_IMAGE_URI + " TEXT NOT NULL,"
				+ TweetsColumns.TWEET_RESULT_TYPE + " TEXT NOT NULL,"
				+ "UNIQUE (" + TweetsColumns.TWEET_ID
				+ ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.NEWS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT," + NewsColumns.NEWS_ID
				+ " TEXT NOT NULL," + NewsColumns.NEWS_TEXT + " TEXT NOT NULL,"
				+ NewsColumns.NEWS_CREATED_AT + " INTEGER NOT NULL,"
				+ NewsColumns.NEWS_RESULT_TYPE + " TEXT NOT NULL,"
				+ NewsColumns.NEWS_NEW + " INTEGER NOT NULL DEFAULT 0,"
				+ "UNIQUE (" + NewsColumns.NEWS_ID + ") ON CONFLICT IGNORE)");

		db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");

		createSessionsSearch(db);
		createSpeakersSearch(db);

		createIndices(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
		int version = oldVersion;

		// switch (version) {
		// case VER_LAUNCH:
		// }

		Log.d(TAG, "after upgrade logic, at version " + version);
		if (version != DATABASE_VERSION) {
			Log.w(TAG, "Destroying old data during upgrade");

			// old tables
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.TYPES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.TAGS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TAGS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.NOTES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SYNC);

			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SESSIONS_SEARCH_INSERT);
			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SESSIONS_SEARCH_DELETE);
			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SESSIONS_SEARCH_UPDATE);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SPEAKERS_SEARCH_INSERT);
			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SPEAKERS_SEARCH_DELETE);
			db.execSQL("DROP TRIGGER IF EXISTS "
					+ Triggers.SPEAKERS_SEARCH_UPDATE);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS_SEARCH);

			db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

			// new tables
			db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSION_TYPES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.TWEETS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.NEWS);

			onCreate(db);
		}
	}

	private static void createSessionsSearch(SQLiteDatabase db) {
		// Using the "porter" tokenizer for simple stemming, so that
		// "frustration" matches "frustrated."

		db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH
				+ " USING fts3(" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SessionsSearchColumns.BODY + " TEXT NOT NULL,"
				+ SessionsSearchColumns.SESSION_ID + " TEXT NOT NULL "
				+ References.SESSION_ID + "," + "UNIQUE ("
				+ SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
				+ "tokenize=porter)");

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT
				+ " AFTER INSERT ON " + Tables.SESSIONS + " BEGIN INSERT INTO "
				+ Qualified.SESSIONS_SEARCH + " " + " VALUES(new."
				+ Sessions.SESSION_ID + ", " + Subquery.SESSIONS_BODY + ");"
				+ " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE
				+ " AFTER DELETE ON " + Tables.SESSIONS + " BEGIN DELETE FROM "
				+ Tables.SESSIONS_SEARCH + " " + " WHERE "
				+ Qualified.SESSIONS_SEARCH_SESSION_ID + "=old."
				+ Sessions.SESSION_ID + ";" + " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE
				+ " AFTER UPDATE ON " + Tables.SESSIONS + " BEGIN UPDATE "
				+ Tables.SESSIONS_SEARCH + " SET " + SessionsSearchColumns.BODY
				+ " = " + Subquery.SESSIONS_BODY + " WHERE "
				+ Qualified.SESSIONS_SEARCH_SESSION_ID + " = old."
				+ Sessions.SESSION_ID + "; END;");
	}

	private static void createSpeakersSearch(SQLiteDatabase db) {
		// Using the "porter" tokenizer for simple stemming, so that
		// "frustration" matches "frustrated."

		db.execSQL("CREATE VIRTUAL TABLE " + Tables.SPEAKERS_SEARCH
				+ " USING fts3(" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SpeakersSearchColumns.BODY + " TEXT NOT NULL,"
				+ SpeakersSearchColumns.SPEAKER_ID + " TEXT NOT NULL "
				+ References.SPEAKER_ID + "," + "UNIQUE ("
				+ SpeakersSearchColumns.SPEAKER_ID + ") ON CONFLICT REPLACE,"
				+ "tokenize=porter)");

		db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_INSERT
				+ " AFTER INSERT ON " + Tables.SPEAKERS + " BEGIN INSERT INTO "
				+ Qualified.SPEAKERS_SEARCH + " " + " VALUES(new."
				+ Speakers.SPEAKER_ID + ", " + Subquery.SPEAKERS_BODY + ");"
				+ " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_DELETE
				+ " AFTER DELETE ON " + Tables.SPEAKERS + " BEGIN DELETE FROM "
				+ Tables.SPEAKERS_SEARCH + " " + " WHERE "
				+ Qualified.SPEAKERS_SEARCH_SPEAKER_ID + "=old."
				+ Speakers.SPEAKER_ID + ";" + " END;");

		db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_UPDATE
				+ " AFTER UPDATE ON " + Tables.SPEAKERS + " BEGIN UPDATE "
				+ Tables.SPEAKERS_SEARCH + " SET " + SpeakersSearchColumns.BODY
				+ " = " + Subquery.SPEAKERS_BODY + " WHERE "
				+ Qualified.SPEAKERS_SEARCH_SPEAKER_ID + " = old."
				+ Speakers.SPEAKER_ID + "; END;");
	}

	private static void createIndices(SQLiteDatabase db) {
		db.execSQL("CREATE INDEX " + Tables.SESSIONS + "_"
				+ Sessions.SESSION_ID + "_IDX ON " + Tables.SESSIONS + "("
				+ Sessions.SESSION_ID + ")");
		db.execSQL("CREATE INDEX " + Tables.SESSIONS + "_" + Sessions.BLOCK_ID
				+ "_IDX ON " + Tables.SESSIONS + "(" + Sessions.BLOCK_ID + ")");
		db.execSQL("CREATE INDEX " + Tables.SESSIONS + "_" + Sessions.ROOM_ID
				+ "_IDX ON " + Tables.SESSIONS + "(" + Sessions.ROOM_ID + ")");
		db.execSQL("CREATE INDEX " + Tables.SESSIONS + "_" + Sessions.TRACK_ID
				+ "_IDX ON " + Tables.SESSIONS + "(" + Sessions.TRACK_ID + ")");
		db.execSQL("CREATE INDEX " + Tables.SESSIONS + "_"
				+ Sessions.SESSION_TYPE_ID + "_IDX ON " + Tables.SESSIONS + "("
				+ Sessions.SESSION_TYPE_ID + ")");
		// db.execSQL("CREATE INDEX "
		// + Tables.SESSIONS + "_" + Sessions.STARRED + "_IDX ON "
		// + Tables.SESSIONS + "(" + Sessions.STARRED + ")");

		db.execSQL("CREATE INDEX " + Tables.SPEAKERS + "_"
				+ Speakers.SPEAKER_ID + "_IDX ON " + Tables.SPEAKERS + "("
				+ Speakers.SPEAKER_ID + ")");

		db.execSQL("CREATE INDEX " + Tables.ROOMS + "_" + Rooms.ROOM_ID
				+ "_IDX ON " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")");

		db.execSQL("CREATE INDEX " + Tables.BLOCKS + "_" + Blocks.BLOCK_ID
				+ "_IDX ON " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")");

		db.execSQL("CREATE INDEX " + Tables.TRACKS + "_" + Tracks.TRACK_ID
				+ "_IDX ON " + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")");

		db.execSQL("CREATE INDEX " + Tables.TAGS + "_" + Tags.TAG_ID
				+ "_IDX ON " + Tables.TAGS + "(" + Tags.TAG_ID + ")");

		db.execSQL("CREATE INDEX " + Tables.SESSIONS_SPEAKERS + "_"
				+ SessionsSpeakers.SESSION_ID + "_IDX ON "
				+ Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SESSION_ID
				+ ")");
		db.execSQL("CREATE INDEX " + Tables.SESSIONS_SPEAKERS + "_"
				+ SessionsSpeakers.SPEAKER_ID + "_IDX ON "
				+ Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SPEAKER_ID
				+ ")");
	}

}
