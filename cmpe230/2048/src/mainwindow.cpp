#include "mainwindow.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QFrame>

static QString scoreBoxStyle(const QString& color) {
    return QString(R"(
        QFrame {
            background: %1;
            border-radius: 6px;
            min-width: 80px;
        }
    )").arg(color);
}

static QString labelStyleTitle() {
    return "color: #eee4da; font-size: 12px; font-weight: bold; font-family: Arial;";
}

static QString labelStyleValue() {
    return "color: white; font-size: 20px; font-weight: bold; font-family: Arial;";
}

static GameMode modeFromIndex(int i) {
    switch (i) {
    case 1:  return GameMode::Unlimited;
    case 2:  return GameMode::Hard;
    default: return GameMode::Normal;
    }
}

MainWindow::MainWindow(QWidget* parent)
    : QWidget(parent) {
    m_logic = new GameLogic(this);
    setupUi();

    connect(m_logic, &GameLogic::boardChanged, this, &MainWindow::updateScores);
    connect(m_logic, &GameLogic::gameWon,      this, &MainWindow::onWon);
    connect(m_logic, &GameLogic::gameLost,     this, &MainWindow::onLost);
}

void MainWindow::setupUi() {
    setWindowTitle("2048");
    setMinimumSize(480, 560);

    QWidget* central = new QWidget(this);
    central->setStyleSheet("background-color: #faf8ef;");

    QVBoxLayout* mainLayout = new QVBoxLayout(central);
    mainLayout->setContentsMargins(16, 16, 16, 16);
    mainLayout->setSpacing(10);

    // Top row: title + score boxes
    QHBoxLayout* topRow = new QHBoxLayout;

    QLabel* title = new QLabel("2048");
    title->setStyleSheet("color: #776e65; font-size: 48px; font-weight: 900; font-family: Arial;");
    topRow->addWidget(title);
    topRow->addStretch();

    auto makeScoreBox = [&](const QString& label, QLabel*& valLabel, const QString& bg) -> QFrame* {
        QFrame* box = new QFrame;
        box->setStyleSheet(scoreBoxStyle(bg));
        QVBoxLayout* bl = new QVBoxLayout(box);
        bl->setContentsMargins(12, 6, 12, 6);
        bl->setSpacing(0);
        QLabel* lbl = new QLabel(label);
        lbl->setStyleSheet(labelStyleTitle());
        lbl->setAlignment(Qt::AlignCenter);
        valLabel = new QLabel("0");
        valLabel->setStyleSheet(labelStyleValue());
        valLabel->setAlignment(Qt::AlignCenter);
        bl->addWidget(lbl);
        bl->addWidget(valLabel);
        return box;
    };

    topRow->addWidget(makeScoreBox("SCORE", m_current_score_label, "#bbada0"));
    topRow->addSpacing(6);
    topRow->addWidget(makeScoreBox("BEST",  m_best_score_label,  "#bbada0"));
    mainLayout->addLayout(topRow);

    // Controls row
    QHBoxLayout* ctrlRow = new QHBoxLayout;
    ctrlRow->setSpacing(8);

    // Mode selector
    m_gameMode_select = new QComboBox;
    m_gameMode_select->addItem("Normal Mode");
    m_gameMode_select->addItem("Unlimited Mode");
    m_gameMode_select->addItem("Hard Mode");
    m_gameMode_select->setStyleSheet(R"(
        QComboBox {
            background: #8f7a66; color: white; border: none;
            border-radius: 4px; padding: 6px 10px;
            font-weight: bold; font-family: Arial; font-size: 13px;
        }
        QComboBox::drop-down { border: none; width: 20px; }
        QComboBox QAbstractItemView { background: #8f7a66; color: white; }
    )");

    // Continue button: shown only after winning in Unlimited mode
    m_continue_btn = new QPushButton("Continue");
    m_continue_btn->setStyleSheet(R"(
        QPushButton {
            background: #edcf72; color: #776e65; border: none;
            border-radius: 4px; padding: 8px 16px;
            font-weight: bold; font-family: Arial; font-size: 14px;
        }
        QPushButton:hover { background: #f0d982; }
        QPushButton:pressed { background: #d9b85e; }
    )");
    m_continue_btn->setVisible(false);

    // Restart button
    m_restart_btn = new QPushButton("Restart (R)");
    m_restart_btn->setStyleSheet(R"(
        QPushButton {
            background: #8f7a66; color: white; border: none;
            border-radius: 4px; padding: 8px 16px;
            font-weight: bold; font-family: Arial; font-size: 14px;
        }
        QPushButton:hover { background: #9f8b77; }
        QPushButton:pressed { background: #7a6555; }
    )");

    // Undo button
    m_undo_btn = new QPushButton("Undo (U)");
    m_undo_btn->setStyleSheet(R"(
        QPushButton {
            background: #bbada0; color: white; border: none;
            border-radius: 4px; padding: 8px 16px;
            font-weight: bold; font-family: Arial; font-size: 14px;
        }
        QPushButton:hover { background: #c9bcb0; }
        QPushButton:pressed { background: #a89a8c; }
        QPushButton:disabled { background: #cdc1b4; color: #e8e0d6; }
    )");
    m_undo_btn->setEnabled(false);

    ctrlRow->addWidget(m_gameMode_select);
    ctrlRow->addStretch();
    ctrlRow->addWidget(m_continue_btn);
    ctrlRow->addWidget(m_undo_btn);
    ctrlRow->addWidget(m_restart_btn);
    mainLayout->addLayout(ctrlRow);

    // Board
    m_board = new BoardWidget(m_logic, this);
    m_board->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    mainLayout->addWidget(m_board, 1);

    // Hint / countdown label
    m_hint_label = new QLabel;
    m_hint_label->setStyleSheet("color: #a09c93; font-size: 12px; font-family: Arial;");
    m_hint_label->setAlignment(Qt::AlignCenter);
    mainLayout->addWidget(m_hint_label);

    // Connections
    connect(m_restart_btn, &QPushButton::clicked, this, &MainWindow::onReset);
    connect(m_undo_btn,    &QPushButton::clicked, this, &MainWindow::onUndo);
    connect(m_continue_btn, &QPushButton::clicked, this, [this]() {
        m_logic->continueAfterWin(); // resume play in Unlimited; emits boardChanged
        m_board->setFocus();
    });
    connect(m_gameMode_select, QOverload<int>::of(&QComboBox::currentIndexChanged),
            this, &MainWindow::onGameModeChange);

    // Periodic UI refresh for the Hard-mode countdown
    m_timer = new QTimer(this);
    m_timer->setInterval(100);
    connect(m_timer, &QTimer::timeout, this, &MainWindow::updateHint);
    m_timer->start();
    updateHint();

    // MainWindow is a plain QWidget (no setCentralWidget), so attach central
    // with a layout or it would have zero size and nothing would be visible
    QVBoxLayout* outer = new QVBoxLayout(this);
    outer->setContentsMargins(0, 0, 0, 0);
    outer->addWidget(central);
}

void MainWindow::updateScores() {
    m_current_score_label->setText(QString::number(m_logic->score()));
    m_best_score_label->setText(QString::number(m_logic->bestScore()));
    // Undo is available only mid-game
    m_undo_btn->setEnabled(m_logic->canUndo() && m_logic->state() == GameState::Playing);
    // The continue button only appears after winning in Unlimited mode.
    m_continue_btn->setVisible(m_logic->state() == GameState::Won &&
                               m_logic->mode()  == GameMode::Unlimited);
    m_board->update();
}

void MainWindow::updateHint() {
    if (m_logic->isHardMode() && m_logic->state() == GameState::Playing) {
        int ms = m_logic->timeRemainingMs();
        double secs = (ms > 0) ? ms / 1000.0 : 0.0;
        m_hint_label->setText(QString("Hard Mode — auto-move in %1s").arg(secs, 0, 'f', 1));
    } else {
        m_hint_label->setText("WASD / Arrow keys to move   ·   U = undo · R = restart");
    }
}

void MainWindow::onReset() {
    // Restart: start a new game (N x M) in the selected mode
    m_logic->setMode(modeFromIndex(m_gameMode_select->currentIndex()));
    m_logic->reset();
    m_board->setFocus();
}

void MainWindow::onUndo() {
    m_logic->undo();
    m_board->setFocus();
}

void MainWindow::onWon() {
    // "You Win!" overlay is drawn by the board. In Normal/Hard the player may
    // restart or switch to another game mode. In Unlimited a continue button is
    // shown to keep playing
    m_board->update();
}

void MainWindow::onLost() {
    m_board->update();
}

void MainWindow::onGameModeChange(int index) {
    // Switching modes continues the current game under the new rules
    m_logic->setMode(modeFromIndex(index));

    // Choosing Unlimited Mode after a win resumes from the current board and score
    if (m_logic->mode() == GameMode::Unlimited && m_logic->state() == GameState::Won)
        m_logic->continueAfterWin();

    updateScores(); // refresh button visibility for the new mode
    m_board->setFocus();
}