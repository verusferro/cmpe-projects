#include "gameLogic.h"
#include "config.h"
#include <algorithm>

GameLogic::GameLogic(QObject* parent)
    : QObject(parent) {
    m_timer = new QTimer(this);
    m_timer->setSingleShot(true);
    connect(m_timer, &QTimer::timeout, this, &GameLogic::autoMove);
    reset();
}

void GameLogic::reset() {
    // Use constants from config.h
    m_rows = Config::N;
    m_cols = Config::M;
    m_board = QVector<QVector<int>>(m_rows, QVector<int>(m_cols, 0));
    m_score = 0;
    m_state = GameState::Playing;
    m_wonAlready = false;
    m_history.clear();
    // Initial 2 tiles
    spawnTile(2);
    spawnTile(2);
    restartTimer();
    emit boardChanged();
}

void GameLogic::setMode(GameMode m) {
    m_mode = m;
    restartTimer(); // entering Hard mode resets the 5s timer
}

void GameLogic::continueAfterWin() {
    m_wonAlready = true; // prevent re-trigger
    m_state = GameState::Playing;
    restartTimer();
    emit boardChanged(); // clear the overlay and resume
}

// Place a tile in a random empty cell. If value is 0, the value is chosen randomly per config
void GameLogic::spawnTile(int value) {
    QVector<QPair<int,int>> emptyCells;
    for (int r = 0; r < m_rows; r++)
        for (int c = 0; c < m_cols; c++)
            if (m_board[r][c] == 0)
                emptyCells.append({r, c});
    if (emptyCells.empty()) return;

    int idx = QRandomGenerator::global()->bounded(emptyCells.size());
    auto [r, c] = emptyCells[idx];
    if (value == 0)
        value = (QRandomGenerator::global()->bounded(100) < Config::P) ? 2 : 4;
    m_board[r][c] = value;
}

// Slides one line toward index 0, merging each equal pair once. Because every
// direction is reduced to "merge toward 0" (callers reverse as needed), the
// pair closest to the leading border merges first
QVector<int> GameLogic::mergeRow(QVector<int> row, int& gained) const {
    QVector<int> compact;
    for (int v : row) if (v != 0) compact.append(v);

    QVector<int> merged;
    for (int i = 0; i < compact.size(); ) {
        if (i + 1 < compact.size() && compact[i] == compact[i+1]) {
            int val = compact[i] * 2;
            merged.append(val);
            gained += val;
            i += 2;
        } else {
            merged.append(compact[i]);
            ++i;
        }
    }
    while ((int)merged.size() < row.size()) merged.append(0);
    return merged;
}

// The board that would result from a slide, without side effects
QVector<QVector<int>> GameLogic::boardAfterMove(int direction, int& gained) const {
    gained = 0;
    QVector<QVector<int>> result = m_board;

    if (direction == 2 || direction == 3) {
        // Row-wise (left / right)
        for (int r = 0; r < m_rows; ++r) {
            QVector<int> row = result[r];
            if (direction == 3) std::reverse(row.begin(), row.end());
            QVector<int> merged = mergeRow(row, gained);
            if (direction == 3) std::reverse(merged.begin(), merged.end());
            result[r] = merged;
        }
    } else {
        // Column-wise (up / down)
        for (int c = 0; c < m_cols; ++c) {
            QVector<int> col;
            for (int r = 0; r < m_rows; ++r) col.append(result[r][c]);
            if (direction == 1) std::reverse(col.begin(), col.end()); // Down
            QVector<int> merged = mergeRow(col, gained);
            if (direction == 1) std::reverse(merged.begin(), merged.end());
            for (int r = 0; r < m_rows; ++r) result[r][c] = merged[r];
        }
    }
    return result;
}

void GameLogic::restartTimer() {
    if (!m_timer) return;
    m_timer->stop();
    if (m_mode == GameMode::Hard && m_state == GameState::Playing)
        m_timer->start(kTimeLimitMs);
}

bool GameLogic::move(int direction) {
    if (m_state != GameState::Playing) return false; // input disabled when over

    int gained = 0;
    QVector<QVector<int>> newBoard = boardAfterMove(direction, gained);
    if (newBoard == m_board) return false; // invalid move: nothing changes, no spawn

    // Snapshot the state before the move for undo
    m_history.append({ m_board, m_score, m_state, m_wonAlready });

    m_board = newBoard;
    m_score += gained;
    if (m_score > m_bestScore) m_bestScore = m_score;

    // Win check: the first time any tile reaches K, the game is "won" in every mode
    // In Normal/Hard this is final (input disabled until restart)
    // In Unlimited the UI shows a Continue button, continueAfterWin() then sets
    // m_wonAlready so play resumes and the win never fires again
    if (!m_wonAlready) {
        for (int r = 0; r < m_rows; ++r)
            for (int c = 0; c < m_cols; ++c)
                if (m_board[r][c] >= Config::K) {
                    m_state = GameState::Won;
                    restartTimer(); // stops the timer (state != Playing)
                    emit boardChanged();
                    emit gameWon();
                    return true;
                }
    }

    spawnTile(); // new random tile after a valid slide

    if (!canMove()) {
        m_state = GameState::Lost;
        restartTimer();
        emit boardChanged();
        emit gameLost();
        return true;
    }

    restartTimer();
    emit boardChanged();
    return true;
}

// All directions that would actually change the board
QVector<int> GameLogic::validMoves() const {
    QVector<int> moves;
    for (int d = 0; d < 4; ++d) {
        int g = 0;
        if (boardAfterMove(d, g) != m_board) moves.append(d);
    }
    return moves;
}

void GameLogic::autoMove() {
    if (m_state != GameState::Playing) return;
    QVector<int> moves = validMoves();
    if (moves.isEmpty()) return;   // nothing valid, next move() will detect loss
    // The random auto-move is chosen only among valid moves
    int dir = moves[QRandomGenerator::global()->bounded(moves.size())];
    move(dir);
}

bool GameLogic::undo() {
    // Undo is disabled once the game has ended, until restart
    if (m_state != GameState::Playing) return false;
    if (m_history.isEmpty()) return false;

    Snapshot s = m_history.takeLast();
    m_board      = s.board;
    m_score      = s.score;
    m_state      = s.state;
    m_wonAlready = s.wonAlready;
    // m_bestScore isn't reverted
    restartTimer(); // undo resets the Hard-mode timer for the current move
    emit boardChanged();
    return true;
}

bool GameLogic::canMove() const {
    return !validMoves().empty();
}