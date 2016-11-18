#! /bin/sh

set -e

while [ $# -gt 0 ]; do
    case $1 in
        help|--help|-h)
            echo "With none params means build ndk"
            echo "exp: $0"
            echo "Use -release to gen streamer out project"
            echo "-v to set the version"
            echo "exp: $0 -release -v v4.0.0"
            exit 0
            ;;
        release|-release)
            RELEASE=1
            ;;
        v|-v)
            CUR_BRANCH=$2
            shift
            ;;
    esac
    shift
done

cd libksylive-arm64
ndk-build clean
ndk-build
cd ..

cd libksylive-armv7a
ndk-build clean
ndk-build
cd ..

cd libksylive-x86
ndk-build clean
ndk-build
cd ..

echo "ndk build success"

if [ -z "$RELEASE" ]; then
    exit 0
fi

KSYSTREAMER_ANDROID_URL=https://github.com/ksvc/KSYStreamer_Android.git
OUT_DIR=KSYStreamer_Android_Release
RELEASE_LIBS=$OUT_DIR/libs
RELEASE_DOC=$OUT_DIR/docs
DEMO_DIR=$OUT_DIR/demo

echo "====================== check KSYStreamer_Android ========"
if [ ! -d "$OUT_DIR" ] ; then
echo "can't find $OUT_DIR, clone again"
git clone ${KSYSTREAMER_ANDROID_URL} $OUT_DIR
fi
cd $OUT_DIR
git fetch origin
git reset --hard
git checkout master
git pull origin master
if [ -n "$CUR_BRANCH" ]; then
git checkout -b $CUR_BRANCH
fi
cd -

echo "====================== copy to  ========"
cp -p ksystreamerdemo/src/main/AndroidManifest.xml $DEMO_DIR/
rm -rf $DEMO_DIR/res
cp -p -r ksystreamerdemo/src/main/res/ $DEMO_DIR/res
rm -rf $DEMO_DIR/src/*
cp -p -r ksystreamerdemo/src/main/java/ $DEMO_DIR/src
rm -rf $RELEASE_LIBS
mkdir -p $RELEASE_LIBS
LIB_DIRS="java arm64 armv7a x86"
for LIB_DIR in $LIB_DIRS
do
cp -p -r libksylive-$LIB_DIR/libs/ $RELEASE_LIBS
done
cd $DEMO_DIR
rm -rf libs
ln -s ../libs libs
cd -
rm -rf $RELEASE_DOC
cp -p -r ../prebuilt/docs/streamer/docs/ $RELEASE_DOC
echo "====================== copy done  ========"
