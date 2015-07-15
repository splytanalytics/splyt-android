package com.rsb.bubblepop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class GameBoard extends TableLayout
{
    public interface GameOverListener
    {
        public void onGameOver();
    }

    List<GameOverListener> mListeners = new ArrayList<GameOverListener>();

    public void addGameOverListener(GameOverListener listener)
    {
        // Store the listener object
        mListeners.add(listener);
    }

    // The game board is a 4 x 4 grid
    private final int NUM_CELLS_X = 4;
    private final int NUM_CELLS_Y = 4;

    // Pseudo-random number generator
    private final Random mRandom;

    private Star mStar;
    private TextView mGameOverTextView;

    private int mNumMoves;

    public GameBoard(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Create the random-number generator
        mRandom = new Random();

        // The game board is initially invisible (but still takes up layout space)
        setVisibility(View.INVISIBLE);
    }

    public int getNumMoves()
    {
        return mNumMoves;
    }

    public int getTotalNumCells()
    {
        return NUM_CELLS_X * NUM_CELLS_Y;
    }

    public void create()
    {
        // Create a grid of bubbles
        for (int rowNum = 0; rowNum < NUM_CELLS_Y; ++rowNum)
        {
            // Add a row where the width is that of the entire table and the height is an equal % of the total number of rows
            final TableRow bubbleRow = new TableRow(getContext());
            bubbleRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 0, 1.0f/NUM_CELLS_Y));
            bubbleRow.setGravity(Gravity.CENTER);
            addView(bubbleRow);

            for (int colNum = 0; colNum < NUM_CELLS_X; ++colNum)
            {
                // Create a new bubble and add it to the game board
                final Bubble bubble = new Bubble(getContext(), mBubbleClickListener);
                bubble.setImageResource(R.drawable.bubble);
                bubbleRow.addView(bubble);

                // Set the size of the bubble such that it fills out an equal % of the width of the row as well as the total height
                // If the table is not square, this will make the bubble an oval, but we take care of that below
                final TableRow.LayoutParams params = (TableRow.LayoutParams)bubble.getLayoutParams();
                params.setMargins(4, 4, 4, 4);
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = 0;
                params.weight = 1.0f/NUM_CELLS_X;

                // Now listen for the global layout event so that we can set the height or width of the bubble to make it a perfect circle
                ViewTreeObserver viewTreeObserver = bubble.getViewTreeObserver();
                if (viewTreeObserver.isAlive())
                {
                    viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
                    {
                        @SuppressWarnings("deprecation")
                        @SuppressLint("NewApi")
                        @Override
                        public void onGlobalLayout()
                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                            {
                                bubble.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                            else
                            {
                                bubble.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }

                            int bubbleHeight = bubble.getMeasuredHeight();
                            int bubbleWidth = bubble.getMeasuredWidth();

                            if (bubbleHeight < bubbleWidth)
                            {
                                // The bubble is wider than it is taller, so set the width to match the height
                                params.width = bubbleHeight;

                                // Reset the weight to 0 as we're specifying the width explicity now
                                params.weight = 0.0f;
                            }
                            else
                            {
                                // The bubble is taller than it is wide, so se the height to match the width
                                params.height = bubbleWidth;
                            }

                            bubble.setLayoutParams(params);
                        }
                    });
                }
            }
        }
    }

    public void reset()
    {
        // Reset the gameboard
        mNumMoves = 0;

        ViewGroup relLayout = (ViewGroup)getParent();
        if (null != mStar)
        {
            // Reset the star
            relLayout.removeView(mStar);
            mStar = null;
        }

        if (null != mGameOverTextView)
        {
            // Reset the game over text
            relLayout.removeView(mGameOverTextView);
            mGameOverTextView = null;
        }
        // This call makes sure the Star won't get rendered one last time even after being removed.
        // Note that calling mStar.clearAnimation() before removing it from the ViewGroup would have had the same effect...
        relLayout.clearDisappearingChildren();

        // Set the gameboard as visible
        setVisibility(View.VISIBLE);

        // Select a random bubble to contain the star
        int cellWithStar = mRandom.nextInt(NUM_CELLS_X * NUM_CELLS_Y);

        // Set all bubbles visible
        int numRows = getChildCount();
        int cellNum = 0;
        for (int rowNum = 0; rowNum < numRows; ++rowNum)
        {
            TableRow bubbleRow = (TableRow)getChildAt(rowNum);
            int numBubblesInRow = bubbleRow.getChildCount();
            for (int bubbleNum = 0; bubbleNum < numBubblesInRow; ++bubbleNum)
            {
                Bubble bubble = (Bubble)bubbleRow.getChildAt(bubbleNum);
                bubble.setVisibility(View.VISIBLE);

                if (cellNum == cellWithStar)
                {
                    // This bubble hides the star
                    bubble.setHidesStar(true);
                }
                else
                {
                    bubble.setHidesStar(false);
                }
                ++cellNum;
            }
        }
    }

    Animation.AnimationListener mStarAnimationListener = new Animation.AnimationListener()
    {
        @Override
        public void onAnimationEnd(Animation animation)
        {
            // The star's finishing move animation is complete, so the game is over.
            // Display some text showing how many moves it took.
            mGameOverTextView = new TextView(getContext());
            String finalWord;
            if (1 == mNumMoves)
            {
                finalWord = "move!";
            }
            else
            {
                finalWord = "moves!";
            }
            mGameOverTextView.setText("Game Completed in " + mNumMoves + " " + finalWord);
            mGameOverTextView.setTextSize(18);

            ViewGroup relLayout = (ViewGroup)getParent();
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = 4;
            lp.addRule(RelativeLayout.BELOW, R.id.uppersection);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            relLayout.addView(mGameOverTextView, lp);

            // Inform the listeners that the game is over
            for (GameOverListener listener : mListeners)
            {
                listener.onGameOver();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {
            // Do nothing
        }

        @Override
        public void onAnimationStart(Animation animation)
        {
            // Do nothing
        }
    };

    View.OnClickListener mBubbleClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mNumMoves++;

            Bubble curBubble = (Bubble)v;
            if (curBubble.getHidesStar())
            {
                // We found the star!  Make all bubbles invisible by setting the GameBoard invisible
                setVisibility(View.INVISIBLE);

                // Get the locations needed to place the star in the RelativeLayout at the same position as the bubble
                // and to translate it to  the center of the GameBoard
                int[] bubbleLoc = new int[2];
                curBubble.getLocationInWindow(bubbleLoc);

                int[] gameBoardLoc = new int[2];
                getLocationInWindow(gameBoardLoc);

                ViewGroup relLayout = (ViewGroup)getParent();
                int[] relLayoutLoc = new int[2];
                relLayout.getLocationInWindow(relLayoutLoc);

                // Calculate the translation delta to translate the star to the center of the GameBoard
                int deltaX = (gameBoardLoc[0] + getWidth() / 2) - (bubbleLoc[0] + curBubble.getWidth() / 2);
                int deltaY = (gameBoardLoc[1] + getHeight() / 2) - (bubbleLoc[1] + curBubble.getHeight() / 2);

                // Create the star and start its animation
                mStar = new Star(getContext());
                mStar.createAndAnimate(curBubble.getWidth(), curBubble.getHeight(), new Point(deltaX, deltaY), mStarAnimationListener);

                // Add the star to the RelativeLayout at the correct position
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                lp.leftMargin = bubbleLoc[0] - relLayoutLoc[0];
                lp.topMargin = bubbleLoc[1] - relLayoutLoc[1];
                relLayout.addView(mStar, lp);
            }
            else
            {
                // The bubble has been clicked, "pop" it
                curBubble.setVisibility(View.INVISIBLE);
            }
        }
    };
}
