package frb.axeron.axsh;

interface IAxshProcess {

    void setWindowSize(long size);

    int waitFor();

    void destroy();
}
