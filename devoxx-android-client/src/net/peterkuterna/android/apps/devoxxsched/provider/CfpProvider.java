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

package net.peterkuterna.android.apps.devoxxsched.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.News;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.ParleysPresentations;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.SearchSuggest;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.SessionTypes;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpContract.Tweets;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.ParleysPresentationsTags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SessionsSearchColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SessionsSpeakers;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SessionsTags;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.SpeakersSearchColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.CfpDatabase.Tables;
import net.peterkuterna.android.apps.devoxxsched.service.CfpSyncService;
import net.peterkuterna.android.apps.devoxxsched.util.SelectionBuilder;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Provider that stores {@link CfpContract} data. Data is usually inserted by
 * {@link CfpSyncService}, and queried by various {@link Activity} instances.
 */
public class CfpProvider extends ContentProvider {

	private static final String TAG = "CfpProvider";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private CfpDatabase mOpenHelper;

	private static final UriMatcher sUriMatcher = buildUriMatcher();

	private static final int BLOCKS = 100;
	private static final int BLOCKS_BETWEEN = 101;
	private static final int BLOCKS_ID = 102;
	private static final int BLOCKS_ID_SESSION = 103;
	private static final int BLOCKS_ID_SESSIONS = 104;

	private static final int TRACKS = 200;
	private static final int TRACKS_ID = 201;
	private static final int TRACKS_ID_SESSIONS = 202;

	private static final int ROOMS = 300;
	private static final int ROOMS_ID = 301;
	private static final int ROOMS_ID_SESSIONS = 302;

	private static final int SESSIONS = 400;
	private static final int SESSIONS_BETWEEN = 401;
	private static final int SESSIONS_STARRED = 402;
	private static final int SESSIONS_NEW = 403;
	private static final int SESSIONS_SEARCH = 404;
	private static final int SESSIONS_PARALLEL = 405;
	private static final int SESSIONS_AT = 406;
	private static final int SESSIONS_ID = 407;
	private static final int SESSIONS_ID_SPEAKERS = 408;
	private static final int SESSIONS_ID_TAGS = 409;
	private static final int SESSIONS_ID_TRACKS = 410;
	private static final int SESSIONS_ID_PARLEYS = 411;

	private static final int SPEAKERS = 500;
	private static final int SPEAKERS_SEARCH = 501;
	private static final int SPEAKERS_ID = 502;
	private static final int SPEAKERS_ID_SESSIONS = 503;

	private static final int TAGS = 600;
	private static final int TAGS_ID = 601;
	private static final int TAGS_ID_SESSIONS = 602;

	private static final int SESSION_TYPES = 700;
	private static final int SESSION_TYPES_ID = 701;

	private static final int TWEETS = 800;
	private static final int TWEETS_ID = 801;

	private static final int NEWS = 900;
	private static final int NEWS_NEW = 901;
	private static final int NEWS_ID = 902;

	private static final int PARLEYS = 1000;
	private static final int PARLEYS_ID = 1001;
	private static final int PARLEYS_ID_TAGS = 1002;

	private static final int SEARCH_SUGGEST = 11000;

	/**
	 * Build and return a {@link UriMatcher} that catches all {@link Uri}
	 * variations supported by this {@link ContentProvider}.
	 */
	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = CfpContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, "blocks", BLOCKS);
		matcher.addURI(authority, "blocks/between/*/*", BLOCKS_BETWEEN);
		matcher.addURI(authority, "blocks/*", BLOCKS_ID);
		matcher.addURI(authority, "blocks/*/session", BLOCKS_ID_SESSION);
		matcher.addURI(authority, "blocks/*/sessions", BLOCKS_ID_SESSIONS);

		matcher.addURI(authority, "tracks", TRACKS);
		matcher.addURI(authority, "tracks/*", TRACKS_ID);
		matcher.addURI(authority, "tracks/*/sessions", TRACKS_ID_SESSIONS);

		matcher.addURI(authority, "rooms", ROOMS);
		matcher.addURI(authority, "rooms/*", ROOMS_ID);
		matcher.addURI(authority, "rooms/*/sessions", ROOMS_ID_SESSIONS);

		matcher.addURI(authority, "sessions", SESSIONS);
		matcher.addURI(authority, "sessions/between/*/*", SESSIONS_BETWEEN);
		matcher.addURI(authority, "sessions/starred", SESSIONS_STARRED);
		matcher.addURI(authority, "sessions/new", SESSIONS_NEW);
		matcher.addURI(authority, "sessions/search/*", SESSIONS_SEARCH);
		matcher.addURI(authority, "sessions/parallel/*", SESSIONS_PARALLEL);
		matcher.addURI(authority, "sessions/at/*", SESSIONS_AT);
		matcher.addURI(authority, "sessions/*", SESSIONS_ID);
		matcher.addURI(authority, "sessions/*/speakers", SESSIONS_ID_SPEAKERS);
		matcher.addURI(authority, "sessions/*/tags", SESSIONS_ID_TAGS);
		matcher.addURI(authority, "sessions/*/tracks", SESSIONS_ID_TRACKS);
		matcher.addURI(authority, "sessions/*/parleys", SESSIONS_ID_PARLEYS);

		matcher.addURI(authority, "speakers", SPEAKERS);
		matcher.addURI(authority, "speakers/search/*", SPEAKERS_SEARCH);
		matcher.addURI(authority, "speakers/*", SPEAKERS_ID);
		matcher.addURI(authority, "speakers/*/sessions", SPEAKERS_ID_SESSIONS);

		matcher.addURI(authority, "tags", TAGS);
		matcher.addURI(authority, "tags/*", TAGS_ID);
		matcher.addURI(authority, "tags/*/sessions", TAGS_ID_SESSIONS);

		matcher.addURI(authority, "sessiontypes", SESSION_TYPES);
		matcher.addURI(authority, "sessiontypes/*", SESSION_TYPES_ID);

		matcher.addURI(authority, "tweets", TWEETS);
		matcher.addURI(authority, "tweets/*", TWEETS_ID);

		matcher.addURI(authority, "news", NEWS);
		matcher.addURI(authority, "news/new", NEWS_NEW);
		matcher.addURI(authority, "news/*", NEWS_ID);

		matcher.addURI(authority, "parleys", PARLEYS);
		matcher.addURI(authority, "parleys/*", PARLEYS_ID);
		matcher.addURI(authority, "parleys/*/tags", PARLEYS_ID_TAGS);

		matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);

		return matcher;
	}

	@Override
	public boolean onCreate() {
		final Context context = getContext();
		mOpenHelper = new CfpDatabase(context);
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case BLOCKS:
			return Blocks.CONTENT_TYPE;
		case BLOCKS_BETWEEN:
			return Blocks.CONTENT_TYPE;
		case BLOCKS_ID:
			return Blocks.CONTENT_ITEM_TYPE;
		case BLOCKS_ID_SESSION:
			return Sessions.CONTENT_ITEM_TYPE;
		case BLOCKS_ID_SESSIONS:
			return Sessions.CONTENT_TYPE;
		case TRACKS:
			return Tracks.CONTENT_TYPE;
		case TRACKS_ID:
			return Tracks.CONTENT_ITEM_TYPE;
		case TRACKS_ID_SESSIONS:
			return Sessions.CONTENT_TYPE;
		case ROOMS:
			return Rooms.CONTENT_TYPE;
		case ROOMS_ID:
			return Rooms.CONTENT_ITEM_TYPE;
		case ROOMS_ID_SESSIONS:
			return Sessions.CONTENT_TYPE;
		case SESSIONS:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_BETWEEN:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_STARRED:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_NEW:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_SEARCH:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_PARALLEL:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_AT:
			return Sessions.CONTENT_TYPE;
		case SESSIONS_ID:
			return Sessions.CONTENT_ITEM_TYPE;
		case SESSIONS_ID_SPEAKERS:
			return Speakers.CONTENT_TYPE;
		case SESSIONS_ID_TAGS:
			return Tags.CONTENT_TYPE;
		case SESSIONS_ID_TRACKS:
			return Tracks.CONTENT_TYPE;
		case SPEAKERS:
			return Speakers.CONTENT_TYPE;
		case SPEAKERS_SEARCH:
			return Speakers.CONTENT_TYPE;
		case SPEAKERS_ID:
			return Speakers.CONTENT_ITEM_TYPE;
		case SPEAKERS_ID_SESSIONS:
			return Sessions.CONTENT_TYPE;
		case TAGS:
			return Tags.CONTENT_TYPE;
		case TAGS_ID:
			return Tags.CONTENT_ITEM_TYPE;
		case TAGS_ID_SESSIONS:
			return Sessions.CONTENT_TYPE;
		case SESSION_TYPES:
			return SessionTypes.CONTENT_TYPE;
		case SESSION_TYPES_ID:
			return SessionTypes.CONTENT_ITEM_TYPE;
		case TWEETS:
			return Tweets.CONTENT_TYPE;
		case TWEETS_ID:
			return Tweets.CONTENT_ITEM_TYPE;
		case NEWS:
			return News.CONTENT_TYPE;
		case NEWS_NEW:
			return News.CONTENT_TYPE;
		case NEWS_ID:
			return News.CONTENT_ITEM_TYPE;
		case PARLEYS:
			return ParleysPresentations.CONTENT_TYPE;
		case PARLEYS_ID:
			return ParleysPresentations.CONTENT_ITEM_TYPE;
		case PARLEYS_ID_TAGS:
			return Tags.CONTENT_TYPE;
		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	/** {@inheritDoc} */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (LOGV)
			Log.v(TAG,
					"query(uri=" + uri + ", proj="
							+ Arrays.toString(projection) + ")");
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		final int match = sUriMatcher.match(uri);
		switch (match) {
		default: {
			// Most cases are handled with simple SelectionBuilder
			final SelectionBuilder builder = buildExpandedSelection(uri, match);
			return builder.where(selection, selectionArgs).query(db,
					projection, sortOrder);
		}
		case SEARCH_SUGGEST: {
			final SelectionBuilder builder = new SelectionBuilder();

			// Adjust incoming query to become SQL text match
			selectionArgs[0] = selectionArgs[0] + "%";
			builder.table(Tables.SEARCH_SUGGEST);
			builder.where(selection, selectionArgs);
			builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
					SearchManager.SUGGEST_COLUMN_TEXT_1);

			projection = new String[] { BaseColumns._ID,
					SearchManager.SUGGEST_COLUMN_TEXT_1,
					SearchManager.SUGGEST_COLUMN_QUERY };

			final String limit = uri
					.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
			return builder.query(db, projection, null, null,
					SearchSuggest.DEFAULT_SORT, limit);
		}
		}
	}

	/** {@inheritDoc} */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGV)
			Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString()
					+ ")");
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case BLOCKS: {
			db.insertOrThrow(Tables.BLOCKS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
		}
		case TRACKS: {
			db.insertOrThrow(Tables.TRACKS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Tracks.buildTrackUri(values.getAsString(Tracks.TRACK_ID));
		}
		case ROOMS: {
			db.insertOrThrow(Tables.ROOMS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
		}
		case SESSIONS: {
			db.insertOrThrow(Tables.SESSIONS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Sessions.buildSessionUri(values
					.getAsString(Sessions.SESSION_ID));
		}
		case SESSIONS_ID_SPEAKERS: {
			db.insertOrThrow(Tables.SESSIONS_SPEAKERS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Speakers.buildSpeakerUri(values
					.getAsString(SessionsSpeakers.SPEAKER_ID));
		}
		case SESSIONS_ID_TAGS: {
			db.insertOrThrow(Tables.SESSIONS_TAGS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Tags.buildTagUri(values.getAsString(SessionsTags.TAG_ID));
		}
		case SPEAKERS: {
			db.insertOrThrow(Tables.SPEAKERS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Speakers.buildSpeakerUri(values
					.getAsString(Speakers.SPEAKER_ID));
		}
		case TAGS: {
			db.insertOrThrow(Tables.TAGS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
		}
		case SESSION_TYPES: {
			db.insertOrThrow(Tables.SESSION_TYPES, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return SessionTypes.buildSessionTypeUri(values
					.getAsString(SessionTypes.SESSION_TYPE_ID));
		}
		case TWEETS: {
			db.insertOrThrow(Tables.TWEETS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Tweets.buildTweetUri(values.getAsString(Tweets.TWEET_ID));
		}
		case NEWS: {
			db.insertOrThrow(Tables.NEWS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return News.buildNewsUri(values.getAsString(News.NEWS_ID));
		}
		case PARLEYS: {
			db.insertOrThrow(Tables.PARLEYS_PRESENTATIONS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Sessions.buildSessionUri(values
					.getAsString(ParleysPresentations.PRESENTATION_ID));
		}
		case PARLEYS_ID_TAGS: {
			db.insertOrThrow(Tables.PARLEYS_PRESENTATIONS_TAGS, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return Tags.buildTagUri(values
					.getAsString(ParleysPresentationsTags.TAG_ID));
		}
		case SEARCH_SUGGEST: {
			db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return SearchSuggest.CONTENT_URI;
		}
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

	/** {@inheritDoc} */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (LOGV)
			Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString()
					+ ")");
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSimpleSelection(uri);
		int retVal = builder.where(selection, selectionArgs).update(db, values);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	/** {@inheritDoc} */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGV)
			Log.v(TAG, "delete(uri=" + uri + ")");
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSimpleSelection(uri);
		int retVal = builder.where(selection, selectionArgs).delete(db);
		getContext().getContentResolver().notifyChange(uri, null);
		return retVal;
	}

	/**
	 * Apply the given set of {@link ContentProviderOperation}, executing inside
	 * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
	 * any single one fails.
	 */
	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			final int numOperations = operations.size();
			final ContentProviderResult[] results = new ContentProviderResult[numOperations];
			for (int i = 0; i < numOperations; i++) {
				results[i] = operations.get(i).apply(this, results, i);
			}
			db.setTransactionSuccessful();
			return results;
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Build a simple {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually enough to support {@link #insert},
	 * {@link #update}, and {@link #delete} operations.
	 */
	private SelectionBuilder buildSimpleSelection(Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case BLOCKS: {
			return builder.table(Tables.BLOCKS);
		}
		case BLOCKS_ID: {
			final String blockId = Blocks.getBlockId(uri);
			return builder.table(Tables.BLOCKS).where(Blocks.BLOCK_ID + "=?",
					blockId);
		}
		case TRACKS: {
			return builder.table(Tables.TRACKS);
		}
		case TRACKS_ID: {
			final String trackId = Tracks.getTrackId(uri);
			return builder.table(Tables.TRACKS).where(Tracks.TRACK_ID + "=?",
					trackId);
		}
		case ROOMS: {
			return builder.table(Tables.ROOMS);
		}
		case ROOMS_ID: {
			final String roomId = Rooms.getRoomId(uri);
			return builder.table(Tables.ROOMS).where(Rooms.ROOM_ID + "=?",
					roomId);
		}
		case SESSIONS: {
			return builder.table(Tables.SESSIONS);
		}
		case SESSIONS_STARRED: {
			return builder.table(Tables.SESSIONS).where(
					Sessions.SESSION_STARRED + "=1");
		}
		case SESSIONS_NEW: {
			return builder.table(Tables.SESSIONS).where(
					Sessions.SESSION_NEW + "=1");
		}
		case SESSIONS_ID: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder.table(Tables.SESSIONS).where(
					Sessions.SESSION_ID + "=?", sessionId);
		}
		case SESSIONS_ID_SPEAKERS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder.table(Tables.SESSIONS_SPEAKERS).where(
					Sessions.SESSION_ID + "=?", sessionId);
		}
		case SESSIONS_ID_TAGS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder.table(Tables.SESSIONS_TAGS).where(
					Sessions.SESSION_ID + "=?", sessionId);
		}
		case SPEAKERS: {
			return builder.table(Tables.SPEAKERS);
		}
		case SPEAKERS_ID: {
			final String speakerId = Speakers.getSpeakerId(uri);
			return builder.table(Tables.SPEAKERS).where(
					Speakers.SPEAKER_ID + "=?", speakerId);
		}
		case TAGS: {
			return builder.table(Tables.TAGS);
		}
		case TAGS_ID: {
			final String tagId = Tags.getTagId(uri);
			return builder.table(Tables.TAGS).where(Tags.TAG_ID + "=?", tagId);
		}
		case SESSION_TYPES: {
			return builder.table(Tables.SESSION_TYPES);
		}
		case SESSION_TYPES_ID: {
			final String sessionTypeId = SessionTypes.getSessionTypeId(uri);
			return builder.table(Tables.SESSION_TYPES).where(
					SessionTypes.SESSION_TYPE_ID + "=?", sessionTypeId);
		}
		case TWEETS: {
			return builder.table(Tables.TWEETS);
		}
		case TWEETS_ID: {
			final String tweetId = Tweets.getTweetId(uri);
			return builder.table(Tables.TWEETS).where(Tweets.TWEET_ID + "=?",
					tweetId);
		}
		case NEWS: {
			return builder.table(Tables.NEWS);
		}
		case NEWS_NEW: {
			return builder.table(Tables.NEWS).where(News.NEWS_NEW + "=1");
		}
		case NEWS_ID: {
			final String newsId = News.getNewsId(uri);
			return builder.table(Tables.NEWS)
					.where(News.NEWS_ID + "=?", newsId);
		}
		case PARLEYS: {
			return builder.table(Tables.PARLEYS_PRESENTATIONS);
		}
		case PARLEYS_ID: {
			final String presentationId = ParleysPresentations
					.getParleysId(uri);
			return builder.table(Tables.PARLEYS_PRESENTATIONS)
					.where(ParleysPresentations.PRESENTATION_ID + "=?",
							presentationId);
		}
		case PARLEYS_ID_TAGS: {
			final String presentationId = ParleysPresentations
					.getParleysId(uri);
			return builder.table(Tables.PARLEYS_PRESENTATIONS_TAGS)
					.where(ParleysPresentations.PRESENTATION_ID + "=?",
							presentationId);
		}
		case SEARCH_SUGGEST: {
			return builder.table(Tables.SEARCH_SUGGEST);
		}
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

	/**
	 * Build an advanced {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually only used by {@link #query}, since it
	 * performs table joins useful for {@link Cursor} data.
	 */
	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
		case BLOCKS: {
			return builder.table(Tables.BLOCKS);
		}
		case BLOCKS_BETWEEN: {
			final List<String> segments = uri.getPathSegments();
			final String startTime = segments.get(2);
			final String endTime = segments.get(3);
			return builder
					.table(Tables.BLOCKS)
					.map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
					.map(Blocks.CONTAINS_STARRED,
							Subquery.BLOCK_CONTAINS_STARRED)
					.where(Blocks.BLOCK_START + ">=?", startTime)
					.where(Blocks.BLOCK_START + "<=?", endTime);
		}
		case BLOCKS_ID: {
			final String blockId = Blocks.getBlockId(uri);
			return builder
					.table(Tables.BLOCKS)
					.map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
					.map(Blocks.CONTAINS_STARRED,
							Subquery.BLOCK_CONTAINS_STARRED)
					.where(Blocks.BLOCK_ID + "=?", blockId);
		}
		case BLOCKS_ID_SESSION: {
			final String blockId = Blocks.getBlockId(uri);
			return builder
					.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS)
					.map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
					.map(Blocks.CONTAINS_STARRED,
							Subquery.BLOCK_CONTAINS_STARRED)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.mapToTable(Sessions.TRACK_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId);
		}
		case BLOCKS_ID_SESSIONS: {
			final String blockId = Blocks.getBlockId(uri);
			return builder
					.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS)
					.map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
					.map(Blocks.CONTAINS_STARRED,
							Subquery.BLOCK_CONTAINS_STARRED)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId);
		}
		case TRACKS: {
			return builder.table(Tables.TRACKS).map(Tracks.SESSIONS_COUNT,
					Subquery.TRACK_SESSIONS_COUNT);
		}
		case TRACKS_ID: {
			final String trackId = Tracks.getTrackId(uri);
			return builder.table(Tables.TRACKS).where(Tracks.TRACK_ID + "=?",
					trackId);
		}
		case TRACKS_ID_SESSIONS: {
			final String trackId = Tracks.getTrackId(uri);
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.mapToTable(Sessions.TRACK_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_TRACK_ID + "=?", trackId);
		}
		case ROOMS: {
			return builder.table(Tables.ROOMS);
		}
		case ROOMS_ID: {
			final String roomId = Rooms.getRoomId(uri);
			return builder.table(Tables.ROOMS).where(Rooms.ROOM_ID + "=?",
					roomId);
		}
		case ROOMS_ID_SESSIONS: {
			final String roomId = Rooms.getRoomId(uri);
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_ROOM_ID + "=?", roomId);
		}
		case SESSIONS: {
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS);
		}
		case SESSIONS_BETWEEN: {
			final List<String> segments = uri.getPathSegments();
			final String startTime = segments.get(2);
			final String endTime = segments.get(3);
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.mapToTable(Sessions.TRACK_ID, Tables.SESSIONS)
					.where(Sessions.BLOCK_START + ">=?", startTime)
					.where(Sessions.BLOCK_END + "<=?", endTime);
		}
		case SESSIONS_STARRED: {
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Sessions.SESSION_STARRED + "=1");
		}
		case SESSIONS_NEW: {
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Sessions.SESSION_NEW + "=1");
		}
		case SESSIONS_SEARCH: {
			final String query = Sessions.getSearchQuery(uri);
			return builder
					.table(Tables.SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS)
					.map(Sessions.SEARCH_SNIPPET, Subquery.SESSIONS_SNIPPET)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(SessionsSearchColumns.BODY + " MATCH ?", query);
		}
		case SESSIONS_PARALLEL: {
			final List<String> segments = uri.getPathSegments();
			final String sessionId = segments.get(2);
			return builder
					.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.map(Sessions.STARRED_IN_BLOCK_COUNT,
							Subquery.BLOCK_STARRED_SESSIONS_COUNT)
					.mapToTable(Tracks.TRACK_COLOR, Tables.TRACKS)
					.where(WhereClause.SESSIONS_PARALLEL, sessionId, sessionId)
					.where(Sessions.SESSION_ID + "<>?", sessionId);
		}
		case SESSIONS_AT: {
			final List<String> segments = uri.getPathSegments();
			final String time = segments.get(2);
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Sessions.BLOCK_START + "<=?", time)
					.where(Sessions.BLOCK_END + ">=?", time);
		}
		case SESSIONS_ID: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.mapToTable(Sessions.TRACK_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
		}
		case SESSIONS_ID_SPEAKERS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder
					.table(Tables.SESSIONS_SPEAKERS_JOIN_SPEAKERS)
					.mapToTable(Speakers._ID, Tables.SPEAKERS)
					.mapToTable(Speakers.SPEAKER_ID, Tables.SPEAKERS)
					.where(Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=?",
							sessionId);
		}
		case SESSIONS_ID_TAGS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder
					.table(Tables.SESSIONS_TAGS_JOIN_TAGS)
					.mapToTable(Tags._ID, Tables.TAGS)
					.mapToTable(Tags.TAG_ID, Tables.TAGS)
					.where(Qualified.SESSIONS_TAGS_SESSION_ID + "=?", sessionId);
		}
		case SESSIONS_ID_TRACKS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder.table(Tables.SESSIONS_JOIN_TRACKS)
					.mapToTable(Tracks._ID, Tables.TRACKS)
					.mapToTable(Tracks.TRACK_ID, Tables.TRACKS)
					.where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
		}
		case SESSIONS_ID_PARLEYS: {
			final String sessionId = Sessions.getSessionId(uri);
			return builder
					.table(Tables.SESSIONS_TAGS_JOIN_PARLAYS_PRESENTATIONS_TAGS_PARLEYS_PRESENTATIONS)
					.groupBy(Qualified.PARLEYS_PRESENTATIONS_PRESENTATION_ID)
					.mapToTable(ParleysPresentations._ID,
							Tables.PARLEYS_PRESENTATIONS)
					.mapToTable(ParleysPresentations.PRESENTATION_ID,
							Tables.PARLEYS_PRESENTATIONS)
					.where(Qualified.PARLEYS_PRESENTATIONS_PRESENTATION_ID
							+ " IS NOT NULL AND "
							+ Qualified.SESSIONS_TAGS_SESSION_ID + "=?",
							sessionId);
		}
		case SPEAKERS: {
			return builder.table(Tables.SPEAKERS);
		}
		case SPEAKERS_SEARCH: {
			final String query = Sessions.getSearchQuery(uri);
			return builder.table(Tables.SPEAKERS_SEARCH_JOIN_SPEAKERS)
					.map(Speakers.SEARCH_SNIPPET, Subquery.SPEAKERS_SNIPPET)
					.mapToTable(Speakers._ID, Tables.SPEAKERS)
					.mapToTable(Speakers.SPEAKER_ID, Tables.SPEAKERS)
					.where(SpeakersSearchColumns.BODY + " MATCH ?", query);
		}
		case SPEAKERS_ID: {
			final String speakerId = Speakers.getSpeakerId(uri);
			return builder.table(Tables.SPEAKERS).where(
					Speakers.SPEAKER_ID + "=?", speakerId);
		}
		case SPEAKERS_ID_SESSIONS: {
			final String speakerId = Speakers.getSpeakerId(uri);
			return builder
					.table(Tables.SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "=?",
							speakerId);
		}
		case TAGS: {
			return builder.table(Tables.TAGS);
		}
		case TAGS_ID: {
			final String tagId = Tags.getTagId(uri);
			return builder.table(Tables.TAGS).where(Tags.TAG_ID + "=?", tagId);
		}
		case TAGS_ID_SESSIONS: {
			final String tagId = Tags.getTagId(uri);
			return builder
					.table(Tables.SESSIONS_TAGS_JOIN_SESSIONS_BLOCKS_ROOMS)
					.mapToTable(Sessions._ID, Tables.SESSIONS)
					.mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
					.mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
					.mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
					.where(Qualified.SESSIONS_TAGS_TAG_ID + "=?", tagId);
		}
		case SESSION_TYPES: {
			return builder.table(Tables.SESSION_TYPES);
		}
		case SESSION_TYPES_ID: {
			final String sessionTypeId = SessionTypes.getSessionTypeId(uri);
			return builder.table(Tables.SESSION_TYPES).where(
					SessionTypes.SESSION_TYPE_ID + "=?", sessionTypeId);
		}
		case TWEETS: {
			return builder.table(Tables.TWEETS);
		}
		case TWEETS_ID: {
			final String tweetId = Tweets.getTweetId(uri);
			return builder.table(Tables.TWEETS).where(Tweets.TWEET_ID + "=?",
					tweetId);
		}
		case NEWS: {
			return builder.table(Tables.NEWS);
		}
		case NEWS_NEW: {
			return builder.table(Tables.NEWS).where(News.NEWS_NEW + "=1");
		}
		case NEWS_ID: {
			final String newsId = News.getNewsId(uri);
			return builder.table(Tables.NEWS)
					.where(News.NEWS_ID + "=?", newsId);
		}
		case PARLEYS: {
			return builder.table(Tables.PARLEYS_PRESENTATIONS);
		}
		case PARLEYS_ID: {
			final String presentationId = ParleysPresentations
					.getParleysId(uri);
			return builder.table(Tables.PARLEYS_PRESENTATIONS).where(
					Qualified.PARLEYS_PRESENTATIONS_PRESENTATION_ID + "=?",
					presentationId);
		}
		case PARLEYS_ID_TAGS: {
			final String presentationId = ParleysPresentations
					.getParleysId(uri);
			return builder
					.table(Tables.PARLEYS_PRESENTATIONS_TAGS_JOIN_TAGS)
					.mapToTable(Tags._ID, Tables.TAGS)
					.mapToTable(Tags.TAG_ID, Tables.TAGS)
					.where(Qualified.PARLEYS_PRESENTATIONS_TAGS_PRESENTATION_ID
							+ "=?", presentationId);
		}
		default: {
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
		}
	}

	private interface Subquery {
		String BLOCK_SESSIONS_COUNT = "(SELECT COUNT("
				+ Qualified.SESSIONS_SESSION_ID + ") FROM " + Tables.SESSIONS
				+ " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
				+ Qualified.BLOCKS_BLOCK_ID + ")";

		String BLOCK_CONTAINS_STARRED = "(SELECT MAX("
				+ Qualified.SESSIONS_STARRED + ") FROM " + Tables.SESSIONS
				+ " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
				+ Qualified.BLOCKS_BLOCK_ID + ")";

		String BLOCK_STARRED_SESSIONS_COUNT = "(SELECT COUNT("
				+ Qualified.S_SESSION_ID + ") FROM " + Tables.SESSIONS
				+ " AS S LEFT OUTER JOIN " + Tables.BLOCKS + " AS B ON "
				+ Qualified.S_BLOCK_ID + "=" + Qualified.B_BLOCK_ID + " WHERE "
				+ Qualified.S_STARRED + "=1 AND " + "(("
				+ Qualified.BLOCKS_BLOCK_START + ">=" + Qualified.B_BLOCK_START
				+ " AND " + Qualified.BLOCKS_BLOCK_END + "<="
				+ Qualified.B_BLOCK_END + ") OR (" + Qualified.BLOCKS_BLOCK_END
				+ ">" + Qualified.B_BLOCK_START + " AND "
				+ Qualified.BLOCKS_BLOCK_END + "<=" + Qualified.B_BLOCK_END
				+ ") OR (" + Qualified.BLOCKS_BLOCK_START + "<"
				+ Qualified.B_BLOCK_END + " AND "
				+ Qualified.BLOCKS_BLOCK_START + ">=" + Qualified.B_BLOCK_START
				+ ")))";

		String TRACK_SESSIONS_COUNT = "(SELECT COUNT("
				+ Qualified.SESSIONS_TRACK_ID + ") FROM " + Tables.SESSIONS
				+ " WHERE " + Qualified.SESSIONS_TRACK_ID + "="
				+ Qualified.TRACKS_TRACK_ID + ")";

		String SESSIONS_SNIPPET = "snippet(" + Tables.SESSIONS_SEARCH
				+ ",'{','}','\u2026')";
		String SPEAKERS_SNIPPET = "snippet(" + Tables.SPEAKERS_SEARCH
				+ ",'{','}','\u2026')";
	}

	private interface WhereClause {
		String SESSIONS_PARALLEL = "(" + Sessions.BLOCK_START + " >= (SELECT "
				+ Blocks.BLOCK_START + " FROM " + Tables.BLOCKS
				+ " LEFT OUTER JOIN " + Tables.SESSIONS + " ON "
				+ Tables.BLOCKS + "." + Blocks.BLOCK_ID + "=" + Tables.SESSIONS
				+ "." + Sessions.BLOCK_ID + " WHERE " + Tables.SESSIONS + "."
				+ Sessions.SESSION_ID + " = ?) AND " + Sessions.BLOCK_END
				+ " <= (SELECT " + Blocks.BLOCK_END + " FROM " + Tables.BLOCKS
				+ " LEFT OUTER JOIN " + Tables.SESSIONS + " ON "
				+ Tables.BLOCKS + "." + Blocks.BLOCK_ID + "=" + Tables.SESSIONS
				+ "." + Sessions.BLOCK_ID + " WHERE " + Tables.SESSIONS + "."
				+ Sessions.SESSION_ID + " = ?))";
	}

	/**
	 * {@link CfpContract} fields that are fully qualified with a specific
	 * parent {@link Tables}. Used when needed to work around SQL ambiguity.
	 */
	private interface Qualified {
		String SESSIONS_SESSION_ID = Tables.SESSIONS + "."
				+ Sessions.SESSION_ID;
		String SESSIONS_BLOCK_ID = Tables.SESSIONS + "." + Sessions.BLOCK_ID;
		String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;
		String SESSIONS_TRACK_ID = Tables.SESSIONS + "." + Sessions.TRACK_ID;

		String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
				+ SessionsSpeakers.SESSION_ID;
		String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
				+ SessionsSpeakers.SPEAKER_ID;
		String SESSIONS_TAGS_SESSION_ID = Tables.SESSIONS_TAGS + "."
				+ SessionsTags.SESSION_ID;
		String SESSIONS_TAGS_TAG_ID = Tables.SESSIONS_TAGS + "."
				+ SessionsTags.TAG_ID;
		String PARLEYS_PRESENTATIONS_TAGS_PRESENTATION_ID = Tables.PARLEYS_PRESENTATIONS_TAGS
				+ "." + ParleysPresentationsTags.PRESENTATION_ID;
		String PARLEYS_PRESENTATIONS_TAGS_TAG_ID = Tables.PARLEYS_PRESENTATIONS_TAGS
				+ "." + ParleysPresentationsTags.TAG_ID;

		@SuppressWarnings("hiding")
		String SESSIONS_STARRED = Tables.SESSIONS + "."
				+ Sessions.SESSION_STARRED;

		String TRACKS_TRACK_ID = Tables.TRACKS + "." + Tracks.TRACK_ID;

		String BLOCKS_BLOCK_ID = Tables.BLOCKS + "." + Blocks.BLOCK_ID;
		String BLOCKS_BLOCK_START = Tables.BLOCKS + "." + Blocks.BLOCK_START;
		String BLOCKS_BLOCK_END = Tables.BLOCKS + "." + Blocks.BLOCK_END;

		String PARLEYS_PRESENTATIONS_PRESENTATION_ID = Tables.PARLEYS_PRESENTATIONS
				+ "." + ParleysPresentations.PRESENTATION_ID;

		String S_SESSION_ID = "S." + Sessions.SESSION_ID;
		String S_BLOCK_ID = "S." + Sessions.BLOCK_ID;
		String S_STARRED = "S." + Sessions.SESSION_STARRED;
		String B_BLOCK_ID = "B." + Blocks.BLOCK_ID;
		String B_BLOCK_START = "B." + Blocks.BLOCK_START;
		String B_BLOCK_END = "B." + Blocks.BLOCK_END;
	}

}
