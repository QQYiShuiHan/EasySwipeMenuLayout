package com.example.hqq.easyswipemunelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HQQ on 2017/9/28.
 */

public class EasySwipeMenuLayout extends ViewGroup {

    private static final String TAG = "EasySwipeMenuLayout";

    /**
     * 宽高中包含（match_parent）的子控件的集合
     */
    private List<View> mMatchParentChildren = new ArrayList<>();
    /**
     * 个子控件的自定义属性
     */
    private int mLeftViewResID;
    private int mRightViewResID;
    private int mContentViewResID;
    private float mFraction = 0.3f;
    private boolean mCanLeftSwipe = true;
    private boolean mCanRightSwipe = true;
    /**
     * 子控件
     */
    private View mLeftView;
    private View mRightView;
    private View mContentView;
    /**
     * 滑动处理器
     */
    private Scroller mScroller;
    /**
     * 滑动的最小感应距离（16）
     */
    private int mScaledTouchSlop;
    /**
     * 中间子控件的params
     */
    private MarginLayoutParams mContentViewLp;
    /**
     * 开始滑动和滑动结束的点
     */
    private PointF mFirstP;
    private PointF mLastP;
    /**
     * 滑动的总距离
     */
    private float mFinallyScrollX;
    private boolean isSwiping = false;
    private static EasySwipeMenuLayout mCacheView;
    /**
     * 控件的开关状态
     */
    private static Enum<State> mMenuState;


    public EasySwipeMenuLayout(Context context) {
        this(context, null);
    }

    public EasySwipeMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasySwipeMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }


    protected void init(Context context, AttributeSet attrs, int defStyleAttr) {
        // 滑动的处理器
        mScroller = new Scroller(context);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        // 可以感应到的最短距离
        mScaledTouchSlop = viewConfiguration.getScaledTouchSlop();
        //1、获取配置的属性值
        TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.EasySwipeMenuLayout, defStyleAttr, 0);

        try {
            int indexCount = typedArray.getIndexCount();

            for (int i = 0; i < indexCount; i++) {
                int attr = typedArray.getIndex(i);
                if (attr == R.styleable.EasySwipeMenuLayout_leftMenuView) {
                    mLeftViewResID = typedArray.getResourceId(R.styleable.EasySwipeMenuLayout_leftMenuView, -1);
                } else if (attr == R.styleable.EasySwipeMenuLayout_rightMenuView) {
                    mRightViewResID = typedArray.getResourceId(R.styleable.EasySwipeMenuLayout_rightMenuView, -1);
                }else if (attr == R.styleable.EasySwipeMenuLayout_contentView){
                    mContentViewResID = typedArray.getResourceId(R.styleable.EasySwipeMenuLayout_contentView, -1);
                }else if (attr == R.styleable.EasySwipeMenuLayout_canLeftSwipe){
                    mCanLeftSwipe = typedArray.getBoolean(R.styleable.EasySwipeMenuLayout_canLeftSwipe, true);
                }else if (attr == R.styleable.EasySwipeMenuLayout_canRightSwipe){
                    mCanRightSwipe = typedArray.getBoolean(R.styleable.EasySwipeMenuLayout_canRightSwipe, true);
                }else if (attr == R.styleable.EasySwipeMenuLayout_fraction){
                    mFraction = typedArray.getFloat(R.styleable.EasySwipeMenuLayout_fraction, 0.5f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 清空子控件集合
        mMatchParentChildren.clear();
        // 子控件个数
        int childCount = getChildCount();
        // 如果子控件中没有watch_parent则值为 true
        final boolean isHasMatchParentChild = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

        int maxWidth = 0;
        int maxHeight = 0;
        int childState = 0;
        // 遍历所有子控件（可见的）
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                // 测量子控件，带有margin属性的
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

                MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();

                maxWidth = Math.max(maxWidth, params.leftMargin + child.getMeasuredWidth() + params.rightMargin);
                maxHeight = Math.max(maxHeight, params.topMargin + child.getMeasuredHeight() + params.bottomMargin);
                // 通过位运算合并所有子空间的state
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                // 将所用的大小不定并且宽高包含match_parent的控件添加到集合
                if (isHasMatchParentChild) {
                    if (params.width == LayoutParams.MATCH_PARENT ||
                            params.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }

        }
        // 有最小宽高作比较，去较大值
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        // 设置测量的控价的宽高数据
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        // 对宽高不定的子控件重行测量
        childCount = mMatchParentChildren.size();

        if (childCount > 1) {
            for (int i = 0; i < childCount; i++) {
                final View child = mMatchParentChildren.get(i);
                final MarginLayoutParams mlp = (MarginLayoutParams) child.getLayoutParams();

                final int childWidthMeasureSpec;
                if (mlp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0, getMeasuredWidth() - mlp.leftMargin - mlp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            mlp.leftMargin + mlp.rightMargin, mlp.width);
                }

                final int childHeightMeasureSpec;
                if (mlp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0, getMeasuredHeight() - mlp.topMargin - mlp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            mlp.topMargin + mlp.bottomMargin, mlp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingL = getPaddingLeft();
        int paddingR = getPaddingRight();
        int paddingT = getPaddingTop();
        int padingbB = getPaddingBottom();

        int childCount = getChildCount();
        // 子控件赋值
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int id = child.getId();
            if (id == mLeftViewResID && mLeftView == null){
                mLeftView = child;
                mLeftView.setClickable(true);
            } else if (id == mRightViewResID && mRightView == null) {
                mRightView = child;
                mRightView.setClickable(true);
            }else if (id == mContentViewResID && mContentView == null){
                mContentView = child;
                mContentView.setClickable(true);
            }
        }
        // 子控件布局
        if (mContentView != null){
            mContentViewLp = (MarginLayoutParams) mContentView.getLayoutParams();
            int cLeft = paddingL + mContentViewLp.leftMargin;
            int cTop = paddingT + mContentViewLp.topMargin;
            int cRight = cLeft + mContentView.getMeasuredWidth();
            int cBottom = cTop + mContentView.getMeasuredHeight();
            mContentView.layout(cLeft, cTop, cRight, cBottom);
        }

        if (mLeftView != null){
            MarginLayoutParams leftViewLp = (MarginLayoutParams) mLeftView.getLayoutParams();
            int lLeft = 0 - mLeftView.getMeasuredWidth() - leftViewLp.rightMargin;
            int lTop = paddingT + leftViewLp.topMargin;
            int lRight = 0 - leftViewLp.rightMargin;
            int lBottom = lTop + mLeftView.getMeasuredHeight();
            mLeftView.layout(lLeft, lTop, lRight, lBottom);
        }

        if (mRightView != null){
            MarginLayoutParams rightViewLp = (MarginLayoutParams) mRightView.getLayoutParams();
            int rLeft = mContentView.getRight() + mContentViewLp.rightMargin + rightViewLp.leftMargin;
            int rTop = paddingT + rightViewLp.topMargin;
            int rRight = rLeft + mRightView.getMeasuredWidth();
            int rBottom = rTop + mRightView.getMeasuredHeight();
            mRightView.layout(rLeft, rTop, rRight, rBottom);
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:

                isSwiping = false;

                if (mFirstP == null){
                    mFirstP  = new PointF();
                }
                mFirstP.set(ev.getRawX(), ev.getRawY());
                if (mLastP == null){
                    mLastP = new PointF();
                }
                mLastP.set(ev.getRawX(), ev.getRawY());

                if (mCacheView != null){
                    if (mCacheView != this){
                        mCacheView.handlerSwipeMenu(State.CLOSE);
                    }
                    // 通知父控件不被拦截
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                // 计算滑动的距离
                float distanceX = mLastP.x - ev.getRawX();
                float distanceY = mLastP.y - ev.getRawY();
                // 如果是竖直滑动不做处理，交由父控件处理
                if (Math.abs(distanceY) > mScaledTouchSlop &&
                        Math.abs(distanceY) > Math.abs(distanceX)){
                    break;
                }
                // 水平滑动
                scrollBy((int) distanceX, 0);
                // 修正滑动界面
                if (getScrollX() < 0){// 左滑
                    if (!mCanRightSwipe || mLeftView == null){
                        scrollTo(0, 0);
                    }else{
                        if (getScrollX() < mLeftView.getLeft()){
                            scrollTo(mLeftView.getLeft(), 0);
                        }
                    }
                }else{// 右滑
                    if (!mCanLeftSwipe || mRightView == null){
                        scrollTo(0, 0);
                    }else{
                        if (getScrollX() > mRightView.getRight() -
                                mContentView.getRight() - mContentViewLp.rightMargin){
                            scrollTo(mRightView.getRight() - mContentView.getRight() - mContentViewLp.rightMargin, 0);
                        }
                    }
                }
                //当处于水平滑动时，禁止父类拦截
                if (Math.abs(distanceX) > mScaledTouchSlop){
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                mLastP.set(ev.getRawX(), ev.getRawY());
                break;
            case MotionEvent.ACTION_UP:
//                break;
            case MotionEvent.ACTION_CANCEL:
                mFinallyScrollX = mFirstP.x - ev.getRawX();
                if (Math.abs(mFinallyScrollX) > mScaledTouchSlop){
                    isSwiping = true;
                }
                mMenuState = getMenuState();
                handlerSwipeMenu(mMenuState);
                break;
            default:

                break;
        }

        return super.dispatchTouchEvent(ev);
    }


    protected void handlerSwipeMenu(Enum<State> state){
        // 打开左侧
        if (state == State.LEFT_OPEN){

            Log.e(TAG, "lefe sx = " + getScrollX() +
                    "    left = " + mLeftView.getLeft() +
                    "    dx = " + (mLeftView.getLeft() - getScrollX()));


            mScroller.startScroll(getScrollX(), 0, mLeftView.getLeft() - getScrollX(), 0);
            mCacheView = this;
            mMenuState = state;
        // 打开右侧
        }else if (state == State.RIGHT_OPEN){

            Log.e(TAG, "right sx = " + getScrollX() +
                    "    mRightView.getRight() = " + mRightView.getRight() +
                    "    mContentView.getRight = " + mContentView.getRight() +
                    "    mContentViewLp.rightMargin  = " + mContentViewLp.rightMargin +
                    "    dx = " + (mRightView.getRight() - mContentView.getRight() - mContentViewLp.rightMargin - getScrollX()));


            mScroller.startScroll(getScrollX(), 0,
                    mRightView.getRight() - mContentView.getRight() - mContentViewLp.rightMargin - getScrollX(), 0);
            mCacheView = this;
            mMenuState = state;
        // 关闭菜单
        }else{

            Log.e(TAG, "sx = " + getScrollX() +
                    "     dx = " + (- getScrollX()));

            mScroller.startScroll(getScrollX(), 0 , -getScrollX(), 0);
            mCacheView = null;
            mMenuState = null;
        }

        invalidate();
    }

    @Override
    public void computeScroll() {
        //判断Scroller是否执行完毕：
        if (mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //通知View重绘-invalidate()->onDraw()->computeScroll()
            invalidate();
        }
    }

    protected Enum<State> getMenuState(){
        if (Math.abs(mFinallyScrollX) < mScaledTouchSlop){
            return mMenuState;
        }
        // 左滑
        if (mFinallyScrollX < 0){
            // 打开左侧菜单
            if(getScrollX() < 0 && mLeftView != null){
                if(Math.abs(getScrollX()) > Math.abs(mLeftView.getWidth()* mFraction)){
                    return State.LEFT_OPEN;
                }
            }
            // 关闭右侧菜单
            if (getScrollX() > 0 && mRightView != null){
                return State.CLOSE;
            }
        } else if (mFinallyScrollX > 0){ // 右滑
            // 打开右侧菜单
            if (getScrollX() > 0 && mRightView != null){
                if (Math.abs(getScrollX()) > Math.abs(mRightView.getWidth()*mFraction)){
                    return State.RIGHT_OPEN;
                }
            }
            // 关闭左侧菜单
            if (getScrollX() < 0 && mLeftView != null){
                return State.CLOSE;
            }
        }
        return State.CLOSE;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                // 滑动时拦截点击事件
                if (Math.abs(mFinallyScrollX) > mScaledTouchSlop){
                    //// 当手指拖动值大于mScaledTouchSlop值时，认为应该进行滚动，拦截子控件的事件
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
//                break;
            case MotionEvent.ACTION_CANCEL:
                //滑动后不触发contentView的点击事件
                if (isSwiping){
                    isSwiping = false;
                    mFinallyScrollX = 0;
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }


    public void resetStatus(){
        if (mCacheView != null){
            if (mMenuState != null && mMenuState != State.CLOSE && mScroller != null){
                mScroller.startScroll(mCacheView.getScrollX(), 0 , -mCacheView.getScrollX(), 0);
                mCacheView.invalidate();
                mCacheView = null;
                mMenuState = null;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this == mCacheView) {
            mCacheView.handlerSwipeMenu(State.CLOSE);
        }
        super.onDetachedFromWindow();
        Log.i(TAG, ">>>>>>>>onDetachedFromWindow");

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this == mCacheView) {
            mCacheView.handlerSwipeMenu(mMenuState);
        }
         Log.i(TAG, ">>>>>>>>onAttachedToWindow");
    }
}


