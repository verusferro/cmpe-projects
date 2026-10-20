#include <QApplication>

#include "mainwindow.h"

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    app.setApplicationName("2048");
    app.setStyle("Fusion");

    MainWindow w;
    w.resize(520, 620);
    w.show();
    return app.exec();
}
