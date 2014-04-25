// Server boot
(
    if (~buffer != nil, {
        ~buffer.close;
        ~buffer.free;
    });
    s.quit;
)

(
    s.boot;
)

// Initialization
(
    TempoClock.default.tempo = 128 / 60;
    ~osc = NetAddr.new("127.0.0.1", 12000);
)

// SynthDefs
(

    SynthDef(\radio, {|outbus=0|
        var radio = SoundIn.ar(0);
        Out.ar(outbus, radio);
    }).add;

    SynthDef(\streamFile, {|outbus=0, bufnum=0|
        var soundFile;
        soundFile = DiskIn.ar(2, bufnum);
        Out.ar(outbus, soundFile);
    }).add;

    SynthDef(\filter, {|outbus=0, t_gate=0, freq=100, rq=0.02, attack=0.001, release=0.08|
        var in, filter, env;
        in = In.ar(outbus, 2);
        filter = LPF.ar(in, freq, rq);
        env = EnvGen.kr(Env.perc(attack, release), t_gate);
        ReplaceOut.ar(outbus, filter * env);
    }).add;

    SynthDef(\limiter, {|outbus=0, scale=1|
        var in, limiter;
        in = In.ar(outbus, 2);
        limiter = Limiter.ar(in * scale, 0.9, 0.001);
        ReplaceOut.ar(outbus, limiter);
    }).add;
)

(
    ~buffer = Buffer.cueSoundFile(s, '/home/joe/Desktop/r1mix_20140221-1930a.wav', 0, 2);


    ~master = MixerChannel(\master, s, 2, 2, level:1);
    ~limiter = ~master.playfx(\limiter, [\scale, 2000]);

    ~audioChannel =  MixerChannel('audio', s, 2, 2, outbus:~master, level:0);
//    ~audio = ~audioChannel.play(\radio);
    ~audio = ~audioChannel.play(\streamFile, [\bufnum, ~buffer.bufnum]);

    ~filterChannel1 = MixerChannel('filter1', s, 2, 2, outbus:~master);
    ~filterChannel2 = MixerChannel('filter2', s, 2, 2, outbus:~master);
    ~filterChannel3 = MixerChannel('filter3', s, 2, 2, outbus:~master);

    ~filter1 = ~filterChannel1.playfx(\filter);
    ~filter2 = ~filterChannel2.playfx(\filter);
    ~filter3 = ~filterChannel3.playfx(\filter);

    ~send1 = ~audioChannel.newPreSend(~filterChannel1, 1);
    ~send2 = ~audioChannel.newPreSend(~filterChannel2, 0.2);
    ~send3 = ~audioChannel.newPreSend(~filterChannel3, 0.2);

    ~bind1 = Pbind(\type, \set,
                   \id, ~filter1.nodeID,
                   \dur, 1,
                   \freq, 100,
                   \t_gate, 1,
                   \processing, Pfunc({~osc.sendMsg("/beat", 0);}),
                   \args, #[\freq, \t_gate]);
    ~bind1.play(clock: TempoClock.default, quant: 1);

    ~bind2 = Pbind(\type, \set,
                   \id, ~filter2.nodeID,
                   \dur, 1,
                   \freq, 10000,
                   \t_gate, 1,
                   \processing, Pfunc({~osc.sendMsg("/beat", 1);}),
                   \args, #[\freq, \t_gate]);
    ~bind2.play(clock: TempoClock.default, quant: [1, 0.5]);

    ~bind3 = Pbind(\type, \set,
                   \id, ~filter3.nodeID,
                   \dur, 2 / 3,
                   \freq, 800,
                   \t_gate, 1,
                   \processing, Pfunc({~osc.sendMsg("/beat", 2);}),
                   \args, #[\freq, \t_gate]);
    ~bind3.play(clock: TempoClock.default, quant: 1);
)


