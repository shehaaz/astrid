/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.json.JSONArray;
import org.weloveastrid.rmilk.MilkUtilities;

import android.text.Spannable;
import android.text.style.ClickableSpan;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Constants;

public class UpdateMessageServiceTest extends DatabaseTestCase {

    @Autowired private StoreObjectDao storeObjectDao;

    public void testNoUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                assertTrue(url, url.contains("language=eng"));
                assertTrue(url.contains("version="));
                return "";
            }
        }.processUpdates();
    }

    public void testIOException() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                fail("should not have displayed updates");
            }

            @Override
            String getUpdates(String url) throws IOException {
                throw new IOException("yayaya");
            }
        }.processUpdates();
    }

    public void testNewUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();
    }

    public void testMultipleUpdates() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("yo"));
                assertFalse(message.getLeft().toString().contains("cat")); // We only process the first update now
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'},{message:'cat'}]";
            }
        }.processUpdates();
    }

    public void testExistingUpdate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("yo"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                fail("should have not displayed again");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithDate() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("yo"));
                assertTrue(message.getLeft().toString().contains("date"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'yo',date:'date'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithInternalPluginOn() {
        clearLatestUpdates();
        MilkUtilities.INSTANCE.setToken("milk");

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("rmilk man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'rmilk man',plugin:'rmilk'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithInternalPluginOff() {
        clearLatestUpdates();
        MilkUtilities.INSTANCE.setToken(null);

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                fail("displayed update");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'rmilk man',plugin:'rmilk'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOn() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertTrue(message.getLeft().toString().contains("astrid man"));
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'" + Constants.PACKAGE + "'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithExternalPluginOff() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                fail("displayed update");
            }

            @Override
            protected void onEmptyMessage() {
                // expected
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{message:'astrid man',plugin:'com.bogus.package'}]";
            }
        }.processUpdates();
    }

    public void testUpdateWithScreenFlow() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertNotNull(message.getRight());
                assertTrue(((Spannable)message).getSpans(0, message.getRight().length(), ClickableSpan.class).length > 0);
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{type:'screen',screens:['com.todoroo.astrid.activity.TaskListActivity'],message:'Screens'}]";
            }
        };
    }

    public void testUpdateWithPrefs() {
        clearLatestUpdates();

        new TestUpdateMessageService() {

            @Override
            void verifyMessage(Pair<String, Spannable> message) {
                assertNotNull(message.getRight());
                assertTrue(((Spannable)message).getSpans(0, message.getRight().length(), ClickableSpan.class).length > 0);
            }

            @Override
            String getUpdates(String url) throws IOException {
                return "[{type:'pref',prefs:[{key:'key', type:'bool', title:'my pref'}],message:'Prefs'}]";
            }
        };
    }

    // ---

    private void clearLatestUpdates() {
        storeObjectDao.deleteWhere(StoreObjectCriteria.byType(UpdateMessageService.UpdateMessage.TYPE));
    }

    /** helper test class */
    abstract public class TestUpdateMessageService extends UpdateMessageService {

        public TestUpdateMessageService() {
            super(null);
            restClient = new RestClient() {

                public String post(String url, HttpEntity data, Header... headers) throws IOException {
                    return null;
                }

                public String get(String url) throws IOException {
                    return getUpdates(url);
                }
            };
        }

        abstract void verifyMessage(Pair<String, Spannable> message);

        abstract String getUpdates(String url) throws IOException;

        protected void onEmptyMessage() {
            fail("empty update message");
        }

        @Override
        protected Pair<String, Spannable> buildUpdateMessage(JSONArray updates) {
            Pair<String, Spannable> message = super.buildUpdateMessage(updates);
            if(message == null || message.getLeft().length() == 0)
                onEmptyMessage();
            return message;
        }

        @Override
        protected void displayUpdateDialog(Pair<String, Spannable> builder) {
            verifyMessage(builder);
        }
    }

}
