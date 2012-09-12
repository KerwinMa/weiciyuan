package org.qii.weiciyuan.othercomponent;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import org.qii.weiciyuan.bean.AccountBean;
import org.qii.weiciyuan.bean.CommentListBean;
import org.qii.weiciyuan.bean.MessageListBean;
import org.qii.weiciyuan.dao.maintimeline.MainCommentsTimeLineDao;
import org.qii.weiciyuan.dao.maintimeline.MainMentionsTimeLineDao;
import org.qii.weiciyuan.support.database.DatabaseManager;
import org.qii.weiciyuan.support.error.WeiboException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Jiang Qi
 * Date: 12-7-31
 */
public class FetchNewMsgService extends Service {
    CommentListBean commentResult;
    MessageListBean repostResult;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new SimpleTask().execute();

        return super.onStartCommand(intent, flags, startId);
    }


    class SimpleTask extends AsyncTask<Void, Void, Map<String, Integer>> {
        WeiboException e;
        AccountBean accountBean;

        @Override
        protected Map<String, Integer> doInBackground(Void... params) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            List<AccountBean> accountBeans = DatabaseManager.getInstance().getAccountList();
            if (accountBeans.size() == 0) {
                cancel(true);
                return null;
            }
            accountBean = accountBeans.get(0);
            String accountId = accountBean.getUid();
            String token = accountBean.getAccess_token();

            CommentListBean commentLineBean = DatabaseManager.getInstance().getCommentLineMsgList(accountId);
            MessageListBean messageListBean = DatabaseManager.getInstance().getRepostLineMsgList(accountId);

            MainCommentsTimeLineDao commentDao = new MainCommentsTimeLineDao(token);
            if (commentLineBean.getSize() > 0) {
                commentDao.setSince_id(commentLineBean.getItemList().get(0).getId());
            }
            try {
                commentResult = commentDao.getGSONMsgList();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return null;
            }
            if (commentResult != null) {
                map.put("comment", commentResult.getSize());
            } else {
                cancel(true);
            }

            MainMentionsTimeLineDao mentionDao = new MainMentionsTimeLineDao(token);
            if (messageListBean.getSize() > 0) {
                mentionDao.setSince_id(messageListBean.getItemList().get(0).getId());
            }
            try {
                repostResult = mentionDao.getGSONMsgList();
            } catch (WeiboException e) {
                cancel(true);
                return null;
            }
            if (repostResult != null) {
                map.put("repost", repostResult.getSize());
            } else {
                cancel(true);
            }
            return map;
        }

        @Override
        protected void onPostExecute(Map<String, Integer> sum) {

            showNotification(sum);
            stopSelf();
            super.onPostExecute(sum);
        }

        @Override
        protected void onCancelled(Map<String, Integer> stringIntegerMap) {
            stopSelf();
            super.onCancelled(stringIntegerMap);
        }

        private void showNotification(Map<String, Integer> sum) {

            Intent intent = new Intent(MentionsAndCommentsReceiver.ACTION);
            intent.putExtra("account", accountBean);
            intent.putExtra("commentsum", sum.get("comment"));
            intent.putExtra("repostsum", sum.get("repost"));
            intent.putExtra("comment", commentResult);
            intent.putExtra("repost", repostResult);
            sendOrderedBroadcast(intent, null);

        }
    }


}
