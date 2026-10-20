#pragma once
#include <QWidget>
#include "gameLogic.h"

class BoardWidget : public QWidget {
    Q_OBJECT
public:
    explicit BoardWidget(GameLogic* logic, QWidget* parent = nullptr);

protected:
    void paintEvent(QPaintEvent*) override;
    void keyPressEvent(QKeyEvent*) override;

private:
    GameLogic* m_logic;
    QColor tileColor(int value) const;
    QColor textColor(int value) const;
    int fontSize(int value) const;
};