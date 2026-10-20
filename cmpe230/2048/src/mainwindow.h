#pragma once
#include <QWidget>
#include <QLabel>
#include <QPushButton>
#include <QComboBox>
#include <QTimer>
#include "gameLogic.h"
#include "boardWidget.h"

class MainWindow : public QWidget {
    Q_OBJECT
public:
    explicit MainWindow(QWidget* parent = nullptr);

private slots:
    void onWon();
    void onLost();
    void onUndo();
    void onReset();
    void onGameModeChange(int index);

private:
    GameLogic*   m_logic;
    BoardWidget* m_board;
    QLabel*      m_best_score_label;
    QLabel*      m_current_score_label;
    QLabel*      m_hint_label;
    QPushButton* m_restart_btn;
    QPushButton* m_undo_btn;
    QPushButton* m_continue_btn;
    QComboBox*   m_gameMode_select;
    QTimer*      m_timer;

    void updateScores();
    void updateHint();
    void setupUi();
};