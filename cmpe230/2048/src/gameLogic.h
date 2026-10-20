#pragma once
#include <QObject>
#include <QVector>
#include <QRandomGenerator>
#include <QTimer>

enum class GameMode { Normal, Unlimited, Hard };
enum class GameState { Won, Lost, Playing };

class GameLogic : public QObject {
    Q_OBJECT
public:
    explicit GameLogic(QObject* parent = nullptr);

    void reset(); // (re)initialize to an N x M board
    bool undo();
    bool canUndo() const { return !m_history.isEmpty(); }
    bool move(int direction); // 0=Up, 1=Down, 2=Left, 3=Right

    int rows() const { return m_rows; }
    int cols() const { return m_cols; }
    int tileAt(int r, int c) const { return m_board[r][c]; }
    int score() const { return m_score; }
    int bestScore() const { return m_bestScore; }
    GameState state() const { return m_state; }
    GameMode mode() const { return m_mode; }
    void setMode(GameMode m);
    void continueAfterWin();
    bool isHardMode() const { return m_mode == GameMode::Hard; }
    int  timeRemainingMs() const {
        return (m_timer && m_timer->isActive()) ? m_timer->remainingTime() : -1;
    }

    static constexpr int kTimeLimitMs = 5000; // Hard-mode auto-move delay

signals:
    void boardChanged();
    void gameWon();
    void gameLost();

public slots:
    void autoMove(); // Hard mode: random valid slide after timeout

private:
    struct Snapshot {
        QVector<QVector<int>> board;
        int       score;
        GameState state;
        bool      wonAlready;
    };

    int m_rows, m_cols;
    QVector<QVector<int>> m_board;
    int m_score = 0;
    int m_bestScore = 0;
    GameState m_state = GameState::Playing;
    GameMode m_mode = GameMode::Normal;
    bool m_wonAlready = false;

    QVector<Snapshot> m_history; // undo stack
    QTimer* m_timer = nullptr;

    void spawnTile(int value = 0); // value 0 = random 2 (P%) / 4 (Q%)
    bool canMove() const;
    QVector<int> mergeRow(QVector<int> row, int& gained) const;
    QVector<QVector<int>> boardAfterMove(int direction, int& gained) const;
    QVector<int> validMoves() const;
    void restartTimer();
};