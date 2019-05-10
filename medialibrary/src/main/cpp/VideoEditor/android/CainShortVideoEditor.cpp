//
// Created by CainHuang on 2019/2/17.
//
extern "C" {
#include <command/ffmpeg.h>
};

#include <editor_log.h>
#include <VideoCutEditor.h>
#include <AudioCutEditor.h>
#include "CainShortVideoEditor.h"

static void processCallback(void *opaque, int type, int time) {
    if (!opaque) {
        LOGD("type = %d, time = %d", type, time);
    } else {
        CainShortVideoEditor *editor = (CainShortVideoEditor *)opaque;
        if (type == EDITOR_PROCESSING) {
            editor->postMessage(EDITOR_PROCESSING, time);
        } else if (type == EDITOR_ERROR) {
            editor->postMessage(EDITOR_ERROR, time);
        }
    }
}

CainShortVideoEditor::CainShortVideoEditor() {
    mListener = nullptr;
    msgThread = nullptr;
    abortRequest = true;
    mFirstMsg = nullptr;
    mLastMsg = nullptr;
    mSize = 0;
}

CainShortVideoEditor::~CainShortVideoEditor() {
    disconnect();
}

void CainShortVideoEditor::init() {
    mMutex.lock();
    abortRequest = false;
    mCondition.signal();
    mMutex.unlock();
    if (!msgThread) {
        msgThread = new Thread(this);
        msgThread->start();
        msgThread->detach();
    }
}

void CainShortVideoEditor::disconnect() {
    mMutex.lock();
    abortRequest = true;
    mCondition.signal();
    mMutex.unlock();
    if (msgThread != nullptr) {
        delete msgThread;
        msgThread = nullptr;
    }
    if (mListener) {
        delete mListener;
        mListener = nullptr;
    }
}

void CainShortVideoEditor::setListener(ShortVideoEditorListener *listener) {
    mMutex.lock();
    if (mListener) {
        delete mListener;
    }
    mListener = listener;
    mCondition.signal();
    mMutex.unlock();
}

int CainShortVideoEditor::execute(int argc, char **argv) {
    register_process_callback(this, processCallback);
    int ret = runCommand(argc, argv);
    register_process_callback(NULL, NULL);
    return ret;
}

/**
 * 处理回调消息
 * @param opaque
 * @param what
 * @param arg1
 * @param arg2
 * @param obj
 * @param len
 */
static void handleMessage(void *opaque, int what, int arg1, int arg2, void *obj, int len) {
    CainShortVideoEditor *editor = (CainShortVideoEditor *)opaque;
    if (editor != nullptr) {
        editor->postMessage(what, arg1, arg2, obj, len);
    }
}

int CainShortVideoEditor::videoCut(const char *srcPath, const char *dstPath, long start, long duration,float speed) {
    Mutex::Autolock lock(mMutex);
    LOGD("video cut start");
    MessageHandle messageHandle;
    memset(&messageHandle, 0, sizeof(MessageHandle));
    messageHandle.opaque = this;
    messageHandle.callback = handleMessage;
    VideoCutEditor editor = VideoCutEditor(srcPath, dstPath, &messageHandle);
    editor.setDuration(start, duration);
    editor.setSpeed(speed);
    return editor.process();
}

int CainShortVideoEditor::audioCut(const char *srcPath, const char *dstPath, long start,
                                   long duration) {
    Mutex::Autolock lock(mMutex);
    LOGD("video cut start");
    MessageHandle messageHandle;
    memset(&messageHandle, 0, sizeof(MessageHandle));
    messageHandle.opaque = this;
    messageHandle.callback = handleMessage;
    AudioCutEditor editor = AudioCutEditor(srcPath, dstPath, &messageHandle);
    editor.setDuration(start, duration);
    return editor.process();
}

void CainShortVideoEditor::postMessage(int what, int arg1, int arg2, void *obj, int len) {
    Mutex::Autolock lock(mMutex);
    Message *message;
    if (abortRequest) {
        return;
    }
    message = (Message *) av_malloc(sizeof(Message));
    if (!message) {
        return;
    }
    message->what = what;
    message->arg1 = arg1;
    message->arg2 = arg2;
    if (obj != NULL && len > 0) {
        message->obj = av_malloc(sizeof(len));
        memcpy(message->obj, obj, len);
    } else {
        message->obj = NULL;
    }
    message->next = NULL;

    if (!mLastMsg) {
        mFirstMsg = message;
    } else {
        mLastMsg->next = message;
    }
    mLastMsg = message;
    mSize++;
    mCondition.signal();
}

int CainShortVideoEditor::getMessage(Message *msg) {
    Message *msg1 = NULL;
    int ret;
    mMutex.lock();
    for (;;) {
        if (abortRequest) {
            ret = -1;
            break;
        }
        msg1 = mFirstMsg;
        if (msg1) {
            mFirstMsg = msg1->next;
            if (!mFirstMsg) {
                mLastMsg = NULL;
            }
            mSize--;
            *msg = *msg1;
            msg1->obj = NULL;
            av_free(msg1);
            ret = 1;
            break;
        } else {
            mCondition.wait(mMutex);
        }
    }
    mMutex.unlock();

    return ret;
}

void CainShortVideoEditor::run() {
    Message message;
    int ret = 0;
    while (true) {
        if (abortRequest) {
            break;
        }
        ret = getMessage(&message);
        if (ret < 0) {
            LOGE("getMessage error");
            break;
        }

        switch (message.what) {
            case EDITOR_PROCESSING: {
                if (mListener != nullptr) {
                    mListener->notify(message.what, message.arg1, message.arg2, message.obj);
                }
                break;
            }
        }
        if (message.obj) {
            av_free(message.obj);
        }
    }
}
