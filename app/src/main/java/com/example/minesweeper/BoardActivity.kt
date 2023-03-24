package com.example.minesweeper

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_board.*
import kotlin.random.Random

class BoardActivity : AppCompatActivity() {
    private lateinit var chronometer: Chronometer

    var choice: Int = 1
    var flaggedMines = 0
    var fastestTime = " NA"
    var lastGameTime = " NA"
    var status = Status.ONGOING
        private set

    enum class Status {
        WON,
        ONGOING,
        LOST
    }

    companion object {
        const val MINE = -1
        val movement = intArrayOf(-1, 0, 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val intent = intent

        // Setting up board according to the option selected in MainActivity

        val level = intent.getStringExtra("selectedLevel")
        if (level.equals("easy")) {
            setUpBoard(10, 10, 10)
        } else if (level.equals("medium")) {
            setUpBoard(16, 16, 40)
        } else if (level.equals("hard")) {
            setUpBoard(30, 16, 99)
        }


        // Restarting the game
        restartGame.setOnClickListener {
            gameRestart()
        }
    }

    private fun setUpBoard(row: Int, col: Int, mine: Int) {

        // Setting up total number of mines
        mineCount.text = "" + mine

        // Array of buttons to find the position of a particular button
        val cellBoard = Array(row) { Array(col) { MineCell(this) } }

        mineFlagOption.setOnClickListener {
            if (choice == 1) {
                mineFlagOption.setImageResource(R.drawable.flag)
                choice = 2
            } else {
                mineFlagOption.setImageResource(R.drawable.bomb)
                choice = 1
            }
        }

        var counter = 1
        var isFirstClick = true

        val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
        )
        val params2 = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
        )

        for (i in 0 until row) {
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.layoutParams = params1
            params1.weight = 1.0F

            for (j in 0 until col) {
                val button = MineCell(this)

                //Buttons saved in the array
                cellBoard[i][j] = button

                button.id = counter
                button.textSize = 18.0F

                button.layoutParams = params2
                params2.weight = 1.0F
                button.setBackgroundResource(R.drawable.ten)
                button.setOnClickListener {

                    // Checking for first click
                    if (isFirstClick) {
                        isFirstClick = false

                        // Setting up mines
                        setMines(i, j, mine, cellBoard, row, col)

                        //Start Timer
                        startTimer()

                    }

                    move(choice, i, j, cellBoard, row, col, mine)
                    display(cellBoard)

                }
                linearLayout.addView(button)
                counter++
            }
            board.addView(linearLayout)
        }
    }

    private fun setMines(row: Int, col: Int, mine: Int, cellBoard: Array<Array<MineCell>>, rowSize: Int, colSize: Int) {
        //Generate random coordinates to set mine
        val mineCount = mine
        var i = 1
        while (i <= mineCount) {
            val r = (Random(System.nanoTime()).nextInt(0, rowSize))
            val c = (Random(System.nanoTime()).nextInt(0, colSize))
            if (r == row || cellBoard[r][c].isMine) {
                continue
            }
            cellBoard[r][c].isMine = true
            cellBoard[r][c].value = -1
            updateNeighbours(r, c, cellBoard, rowSize, colSize)
            i++;
        }
    }

    private fun updateNeighbours(row: Int, column: Int, cellBoard: Array<Array<MineCell>>, rowSize: Int, colSize: Int) {
        for (i in movement) {
            for (j in movement) {
                if (((row + i) in 0 until rowSize) && ((column + j) in 0 until colSize) && cellBoard[row + i][column + j].value != MINE)
                    cellBoard[row + i][column + j].value++
            }
        }
    }

    private fun startTimer() {
        chronometer = findViewById(R.id.timer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
    }

    private fun move(choice: Int, x: Int, y: Int, cellBoard: Array<Array<MineCell>>, rowSize: Int, colSize: Int, mine: Int): Boolean {

        if (choice == 1) {
            if (cellBoard[x][y].isMarked || cellBoard[x][y].isRevealed) {
                return false
            }
            if (cellBoard[x][y].value == MINE) {
                status = Status.LOST;
                updateScore()
                return true
            } else if (cellBoard[x][y].value > 0) {
                cellBoard[x][y].isRevealed = true
                checkStatus(cellBoard, rowSize, colSize);
                return true
            } else if (cellBoard[x][y].value == 0) {
                handleZero(x, y, cellBoard, rowSize, colSize)
                checkStatus(cellBoard, rowSize, colSize);
                return true
            }

        }
        if (choice == 2) {

            if (cellBoard[x][y].isRevealed) return false
            else if (cellBoard[x][y].isMarked) {
                flaggedMines--
                cellBoard[x][y].setBackgroundResource(R.drawable.ten)
                cellBoard[x][y].isMarked = false
                checkStatus(cellBoard, rowSize, colSize)
            } else {
                if (flaggedMines == mine) {
                    Toast.makeText(this, "You cannot mark more than $mine mines", Toast.LENGTH_LONG).show()
                    return false
                }
                flaggedMines++
                cellBoard[x][y].isMarked = true;
                checkStatus(cellBoard, rowSize, colSize)
            }
            val finalMineCount = mine - flaggedMines
            mineCount.text = "" + finalMineCount
            return true;
        }

        return false
    }

    private val xDir = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    private val yDir = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private fun handleZero(x: Int, y: Int, cellBoard: Array<Array<MineCell>>, rowSize: Int, colSize: Int) {

        cellBoard[x][y].isRevealed = true
        for (i in 0..7) {
            val xstep = x + xDir[i]
            val ystep = y + yDir[i]
            if ((xstep < 0 || xstep >= rowSize) || (ystep < 0 || ystep >= colSize)) {
                continue;
            }
            if (cellBoard[xstep][ystep].value > 0 && !cellBoard[xstep][ystep].isMarked) {
                cellBoard[xstep][ystep].isRevealed = true
            } else if (!cellBoard[xstep][ystep].isRevealed && !cellBoard[xstep][ystep].isMarked && cellBoard[xstep][ystep].value == 0) {
                handleZero(xstep, ystep, cellBoard, rowSize, colSize)

            }
        }

    }

    private fun checkStatus(cellBoard: Array<Array<MineCell>>, rowSize: Int, colSize: Int) {
        var flag1 = 0
        var flag2 = 0
        for (i in 0 until rowSize) {
            for (j in 0 until colSize) {
                if (cellBoard[i][j].value == MINE && !cellBoard[i][j].isMarked) {
                    flag1 = 1
                }
                if (cellBoard[i][j].value != MINE && !cellBoard[i][j].isRevealed) {
                    flag2 = 1
                }
            }
        }
        if (flag1 == 0 || flag2 == 0) status = Status.WON
        else status = Status.ONGOING

        if (status == Status.WON) updateScore()

    }

    private fun gameRestart() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setMessage("Do you want to restart the game ?")
        builder.setTitle("Alert!")
        builder.setCancelable(false)

        builder.setPositiveButton("Yes"
        ) { dialog, which ->
            val intent = getIntent()
            finish()
            startActivity(intent)
        }

        builder.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
            }
        })

        val alertDialog = builder.create()
        alertDialog.show()
    }


    override fun onBackPressed() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setMessage("Are you sure you want to exit the game?")
        builder.setTitle("Game is still ongoing!")
        builder.setCancelable(false)

        builder.setPositiveButton("Yes"
        ) { dialog, which ->
            updateScore()
            toMainActivity()
            finish()
            super.onBackPressed()
        }

        builder.setNegativeButton("No", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
            }
        })

        val alertDialog = builder.create()
        alertDialog.show()
    }


    private fun updateScore() {
        chronometer.stop()

        // Getting elapsed time from chronometer
        val elapsedTime = SystemClock.elapsedRealtime() - chronometer.base;
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val lastTime = elapsedTime.toInt()

        // Setting up highscore
        var highScore = sharedPref.getInt(getString(R.string.saved_high_score_key), Integer.MAX_VALUE)

        var isHighScore = false

        // Comparing high score if the last game's status is won
        if (status == Status.WON) {
            if (lastTime < highScore) {
                highScore = lastTime
                isHighScore = true
            }
            with(sharedPref.edit()) {
                putInt(getString(R.string.saved_high_score_key), highScore)
                putInt(getString(R.string.last_time), lastTime)
                commit()
            }
            // Setting time formats to send to another activity
            lastGameTime = "" + ((lastTime / 1000) / 60) + " m " + ((lastTime / 1000) % 60) + " s"
        } else {
            lastGameTime = " Lost!"
            fastestTime = " NA"
        }

        if (highScore == Integer.MAX_VALUE) {
            fastestTime = " NA"
        } else {
            // Setting time formats to send to another activity
            fastestTime = "" + ((highScore / 1000) / 60) + " m " + ((highScore / 1000) % 60) + " s";
        }
        Log.d("MainActivity", "inside savetime " + fastestTime + " " + lastGameTime)

        if (status == Status.WON) {
            gameWon(isHighScore)
        } else if (status == Status.LOST) {
            gameLost()
        }

    }

    private fun gameLost() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        builder.setMessage("Sorry you loose")
        builder.setTitle("Stepped on mine!")
        builder.setCancelable(false)

        builder.setPositiveButton("Restart Game"
        ) { dialog, which ->
            val intent = intent
            finish()
            startActivity(intent)
        }

        builder.setNegativeButton("Main Page", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                finish()
                toMainActivity()
            }
        })

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun gameWon(isHighScore: Boolean) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        // Setting message after checking for highscore
        if (isHighScore) builder.setMessage("$fastestTime is the fastest time")
        else builder.setMessage("$lastGameTime is your time")

        builder.setTitle("Congratulations! You Won")
        builder.setCancelable(false)

        builder.setPositiveButton("Restart Game"
        ) { dialog, which ->
            val intent = intent
            finish()
            startActivity(intent)
        }

        builder.setNegativeButton("Main Page", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                toMainActivity()
                finish()
            }
        })

        val alertDialog = builder.create()
        alertDialog.show()

    }

    private fun toMainActivity() {
        Log.d("MainActivity", "inside to main" + fastestTime + " " + lastGameTime)
        val intent = Intent(this@BoardActivity, MainActivity::class.java)
        intent.putExtra("highScore", fastestTime)
        intent.putExtra("lastTime", lastGameTime)
        startActivity(intent)
    }

    private fun display(cellBoard: Array<Array<MineCell>>) {
        cellBoard.forEach { row ->
            row.forEach {
                if (it.isRevealed)
                    setNumberImage(it)
                else if (it.isMarked)
                    it.setBackgroundResource(R.drawable.flag)
                else if (status == Status.LOST && it.value == MINE) {
                    it.setBackgroundResource(R.drawable.bomb)
                }
                //To show that mine is not present here but it is marked
                if (status == Status.LOST && it.isMarked && !it.isMine) {
                    it.setBackgroundResource(R.drawable.crossedflag)
                } else if (status == Status.WON && it.value == MINE) {
                    it.setBackgroundResource(R.drawable.flag)
                } else
                    it.text = " "
            }

        }
    }

    private fun setNumberImage(button: MineCell) {
        if (button.value == 0) button.setBackgroundResource(R.drawable.zero)
        if (button.value == 1) button.setBackgroundResource(R.drawable.one)
        if (button.value == 2) button.setBackgroundResource(R.drawable.two)
        if (button.value == 3) button.setBackgroundResource(R.drawable.three)
        if (button.value == 4) button.setBackgroundResource(R.drawable.four)
        if (button.value == 5) button.setBackgroundResource(R.drawable.five)
        if (button.value == 6) button.setBackgroundResource(R.drawable.six)
        if (button.value == 7) button.setBackgroundResource(R.drawable.seven)
        if (button.value == 8) button.setBackgroundResource(R.drawable.eight)

    }
}