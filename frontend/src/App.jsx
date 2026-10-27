import { useEffect, useState } from "react";
import { api } from "./api";

const fmt = (s) => new Date(s).toLocaleString([], { dateStyle: "medium", timeStyle: "short" });

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user") || "null"));
  const [msg, setMsg] = useState(null);

  const notify = (text, error = false) => {
    setMsg({ text, error });
    setTimeout(() => setMsg(null), 3500);
  };
  const login = (u) => { localStorage.setItem("user", JSON.stringify(u)); setUser(u); };
  const logout = () => { localStorage.removeItem("user"); setUser(null); };

  return (
    <div className="container">
      <header>
        <h1>🩺 MediBook</h1>
        {user && (
          <div className="who">
            {user.name} <span className="badge">{user.role}</span>
            <button className="ghost" onClick={logout}>Logout</button>
          </div>
        )}
      </header>
      {msg && <div className={"toast " + (msg.error ? "err" : "ok")}>{msg.text}</div>}
      {!user ? <Auth onLogin={login} notify={notify} />
        : user.role === "PATIENT" ? <Patient user={user} notify={notify} />
        : user.role === "DOCTOR" ? <Doctor user={user} notify={notify} />
        : <Admin notify={notify} />}
    </div>
  );
}

/* ---------------- AUTH ---------------- */
function Auth({ onLogin, notify }) {
  const [mode, setMode] = useState("login");
  const [f, setF] = useState({ name: "", email: "", password: "", role: "PATIENT", specialization: "" });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    try {
      if (mode === "register") {
        await api.post("/auth/register", f);
        notify("Registered! Logging you in...");
      }
      onLogin(await api.post("/auth/login", { email: f.email, password: f.password }));
    } catch (err) { notify(err.message, true); }
  };

  return (
    <div className="card narrow">
      <h2>{mode === "login" ? "Login" : "Create account"}</h2>
      <form onSubmit={submit}>
        {mode === "register" && <input placeholder="Full name" value={f.name} onChange={set("name")} required />}
        <input type="email" placeholder="Email" value={f.email} onChange={set("email")} required />
        <input type="password" placeholder="Password (min 6)" value={f.password} onChange={set("password")} required />
        {mode === "register" && (
          <>
            <select value={f.role} onChange={set("role")}>
              <option value="PATIENT">I am a patient</option>
              <option value="DOCTOR">I am a doctor</option>
            </select>
            {f.role === "DOCTOR" && <input placeholder="Specialization" value={f.specialization} onChange={set("specialization")} />}
          </>
        )}
        <button type="submit">{mode === "login" ? "Login" : "Register"}</button>
      </form>
      <p className="link" onClick={() => setMode(mode === "login" ? "register" : "login")}>
        {mode === "login" ? "New here? Register" : "Have an account? Login"}
      </p>
      <div className="demo">
        <b>Demo logins</b><br />
        patient@demo.com / patient123<br />
        doctor@demo.com / doctor123<br />
        admin@demo.com / admin123
      </div>
    </div>
  );
}

/* ---------------- PATIENT ---------------- */
function Patient({ user, notify }) {
  const [doctors, setDoctors] = useState([]);
  const [doctorId, setDoctorId] = useState("");
  const [slots, setSlots] = useState([]);
  const [appts, setAppts] = useState([]);
  const [resId, setResId] = useState(null);

  const loadAppts = () => api.get(`/appointments/patient/${user.id}`).then(setAppts).catch((e) => notify(e.message, true));
  const loadSlots = (id) => id ? api.get(`/slots?doctorId=${id}`).then(setSlots).catch((e) => notify(e.message, true)) : setSlots([]);

  useEffect(() => { api.get("/doctors").then(setDoctors); loadAppts(); }, []);
  useEffect(() => { loadSlots(doctorId); }, [doctorId]);

  const pick = async (slotId) => {
    try {
      if (resId) {
        await api.put(`/appointments/${resId}/reschedule`, { newSlotId: slotId });
        notify("Appointment rescheduled"); setResId(null);
      } else {
        await api.post("/appointments", { slotId, patientId: user.id });
        notify("Appointment booked!");
      }
      loadAppts();
    } catch (e) { notify(e.message, true); }
    loadSlots(doctorId);
  };

  const cancel = async (id) => {
    try { await api.put(`/appointments/${id}/cancel`); notify("Appointment cancelled"); loadAppts(); loadSlots(doctorId); }
    catch (e) { notify(e.message, true); }
  };

  const startReschedule = (a) => { setResId(a.id); setDoctorId(String(a.slot.doctor.id)); window.scrollTo(0, 0); };

  return (
    <>
      <div className="card">
        <h2>{resId ? "Pick a new slot to reschedule" : "Book an appointment"}</h2>
        {resId && <div className="banner">Rescheduling mode <button className="ghost" onClick={() => setResId(null)}>Cancel</button></div>}
        <select value={doctorId} onChange={(e) => setDoctorId(e.target.value)}>
          <option value="">-- Select a doctor --</option>
          {doctors.map((d) => <option key={d.id} value={d.id}>Dr. {d.name} {d.specialization ? `(${d.specialization})` : ""}</option>)}
        </select>
        <div className="slots">
          {doctorId && slots.length === 0 && <p className="muted">No free slots for this doctor.</p>}
          {slots.map((s) => (
            <button key={s.id} className="slot" onClick={() => pick(s.id)}>{fmt(s.startTime)}</button>
          ))}
        </div>
      </div>

      <div className="card">
        <h2>My appointments</h2>
        <table>
          <thead><tr><th>Doctor</th><th>When</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {appts.map((a) => (
              <tr key={a.id}>
                <td>Dr. {a.slot.doctor.name}</td>
                <td>{fmt(a.slot.startTime)}</td>
                <td><span className={"pill " + a.status}>{a.status}</span></td>
                <td>
                  {a.status === "BOOKED" && (
                    <>
                      <button className="small" onClick={() => startReschedule(a)}>Reschedule</button>{" "}
                      <button className="small danger" onClick={() => cancel(a.id)}>Cancel</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
            {appts.length === 0 && <tr><td colSpan="4" className="muted">No appointments yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}

/* ---------------- DOCTOR ---------------- */
function Doctor({ user, notify }) {
  const [slots, setSlots] = useState([]);
  const [appts, setAppts] = useState([]);
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");

  const load = () => {
    api.get(`/slots/doctor/${user.id}`).then(setSlots).catch((e) => notify(e.message, true));
    api.get(`/appointments/doctor/${user.id}`).then(setAppts).catch((e) => notify(e.message, true));
  };
  useEffect(load, []);

  const add = async (e) => {
    e.preventDefault();
    try {
      await api.post("/slots", { doctorId: user.id, startTime: start, endTime: end });
      notify("Slot added"); setStart(""); setEnd(""); load();
    } catch (err) { notify(err.message, true); }
  };
  const remove = async (id) => {
    try { await api.del(`/slots/${id}`); notify("Slot removed"); load(); }
    catch (err) { notify(err.message, true); }
  };

  return (
    <>
      <div className="card">
        <h2>Set availability</h2>
        <form className="row" onSubmit={add}>
          <label>From <input type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} required /></label>
          <label>To <input type="datetime-local" value={end} onChange={(e) => setEnd(e.target.value)} required /></label>
          <button type="submit">Add slot</button>
        </form>
        <table>
          <thead><tr><th>Start</th><th>End</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {slots.map((s) => (
              <tr key={s.id}>
                <td>{fmt(s.startTime)}</td><td>{fmt(s.endTime)}</td>
                <td><span className={"pill " + (s.booked ? "BOOKED" : "FREE")}>{s.booked ? "BOOKED" : "FREE"}</span></td>
                <td>{!s.booked && <button className="small danger" onClick={() => remove(s.id)}>Delete</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="card">
        <h2>My patients</h2>
        <table>
          <thead><tr><th>Patient</th><th>When</th><th>Status</th></tr></thead>
          <tbody>
            {appts.map((a) => (
              <tr key={a.id}><td>{a.patient.name}</td><td>{fmt(a.slot.startTime)}</td><td><span className={"pill " + a.status}>{a.status}</span></td></tr>
            ))}
            {appts.length === 0 && <tr><td colSpan="3" className="muted">No appointments yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  );
}

/* ---------------- ADMIN ---------------- */
function Admin({ notify }) {
  const [stats, setStats] = useState({});
  const [appts, setAppts] = useState([]);

  useEffect(() => {
    api.get("/admin/stats").then(setStats).catch((e) => notify(e.message, true));
    api.get("/admin/appointments").then(setAppts).catch((e) => notify(e.message, true));
  }, []);

  return (
    <>
      <div className="stats">
        {[["Total", "totalAppointments"], ["Booked", "booked"], ["Cancelled", "cancelled"], ["Doctors", "doctors"], ["Patients", "patients"]].map(([label, key]) => (
          <div className="stat" key={key}><div className="num">{stats[key] ?? "-"}</div>{label}</div>
        ))}
      </div>
      <div className="card">
        <h2>All appointments</h2>
        <table>
          <thead><tr><th>Patient</th><th>Doctor</th><th>When</th><th>Status</th></tr></thead>
          <tbody>
            {appts.map((a) => (
              <tr key={a.id}><td>{a.patient.name}</td><td>Dr. {a.slot.doctor.name}</td><td>{fmt(a.slot.startTime)}</td><td><span className={"pill " + a.status}>{a.status}</span></td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
