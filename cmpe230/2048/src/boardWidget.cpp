#include "boardWidget.h"
#include <QPainter>
#include <QKeyEvent>
#include <cmath>

static const int PADDING = 12;
static const int RADIUS  = 10;

BoardWidget::BoardWidget(GameLogic* logic, QWidget* parent)
    : QWidget(parent), m_logic(logic) {
    setFocusPolicy(Qt::StrongFocus);
    setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
}

QColor BoardWidget::tileColor(int value) const {
    switch(value) {
    case 0:    return QColor(0xCDC1B4);
    case 2:    return QColor(0xEEE4DA);
    case 4:    return QColor(0xEDE0C8);
    case 8:    return QColor(0xF2B179);
    case 16:   return QColor(0xF59563);
    case 32:   return QColor(0xF67C5F);
    case 64:   return QColor(0xF65E3B);
    case 128:  return QColor(0xEDCF72);
    case 256:  return QColor(0xEDCC61);
    case 512:  return QColor(0xEDC850);
    case 1024: return QColor(0xEDC53F);
    case 2048: return QColor(0xEDC22E);
    default:
        if (value >= 4096) return QColor(0x3C3A32);
        return QColor(0x776E65);
    }
}

QColor BoardWidget::textColor(int value) const {
    return (value <= 4) ? QColor(0x776E65) : QColor(0xF9F6F2);
}

int BoardWidget::fontSize(int value) const {
    int digits = (value > 0) ? (int)std::log10(value) + 1 : 1;
    if (digits <= 2) return 36;
    if (digits == 3) return 28;
    if (digits == 4) return 22;
    return 16;
}

void BoardWidget::paintEvent(QPaintEvent*) {
    QPainter p(this);
    p.setRenderHint(QPainter::Antialiasing);
    int w = width(), h = height();
    int rows = m_logic->rows(), cols = m_logic->cols();

    // Board background
    p.setBrush(QColor(0xBBADA0));
    p.setPen(Qt::NoPen);
    p.drawRoundedRect(rect(), RADIUS+2, RADIUS+2);

    // Square cell size that fits the available area
    float cellW = (float)(w - PADDING*(cols+1)) / cols;
    float cellH = (float)(h - PADDING*(rows+1)) / rows;
    float cellSize = std::min(cellW, cellH);
    float xOff = (w - (cellSize*cols + PADDING*(cols-1+2))) / 2.0f + PADDING;
    float yOff = (h - (cellSize*rows + PADDING*(rows-1+2))) / 2.0f + PADDING;

    for (int r = 0; r < rows; ++r) {
        for (int c = 0; c < cols; ++c) {
            float x = xOff + c*(cellSize+PADDING);
            float y = yOff + r*(cellSize+PADDING);
            QRectF cellRect(x, y, cellSize, cellSize);
            int val = m_logic->tileAt(r, c);
            p.setBrush(tileColor(val));
            p.setPen(Qt::NoPen);
            p.drawRoundedRect(cellRect, RADIUS, RADIUS);
            if (val > 0) {
                QFont font("Arial", fontSize(val), QFont::Bold);
                p.setFont(font);
                p.setPen(textColor(val));
                p.drawText(cellRect, Qt::AlignCenter, QString::number(val));
            }
        }
    }

    // Overlay for won/lost
    GameState state = m_logic->state();
    if (state == GameState::Won || state == GameState::Lost) {
        QColor overlay = (state == GameState::Won)
        ? QColor(237, 194, 46, 180)
        : QColor(119, 110, 101, 180);
        p.setBrush(overlay);
        p.setPen(Qt::NoPen);
        p.drawRoundedRect(rect(), RADIUS+2, RADIUS+2);
        p.setPen(QColor(0xF9F6F2));
        QFont bigFont("Arial", 42, QFont::Bold);
        p.setFont(bigFont);
        QString msg = (state == GameState::Won) ? "You Win!" : "Game Over";
        p.drawText(rect(), Qt::AlignCenter, msg);
    }

}

void BoardWidget::keyPressEvent(QKeyEvent* e) {
    // Undo/Restart shortcuts.
    switch (e->key()) {
    case Qt::Key_U: m_logic->undo(); return;
    case Qt::Key_R: m_logic->reset(); return;
    default: break;
    }

    // Sliding: arrow keys or WASD
    int dir = -1;
    switch (e->key()) {
    case Qt::Key_Up:    case Qt::Key_W: dir = 0; break;
    case Qt::Key_Down:  case Qt::Key_S: dir = 1; break;
    case Qt::Key_Left:  case Qt::Key_A: dir = 2; break;
    case Qt::Key_Right: case Qt::Key_D: dir = 3; break;
    default: QWidget::keyPressEvent(e); return;
    }
    m_logic->move(dir);
}