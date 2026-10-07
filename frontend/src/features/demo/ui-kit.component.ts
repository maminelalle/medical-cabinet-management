import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PersonnalisationComponent } from './personnalisation.component';

interface Swatch { label: string; token: string; }
interface Kpi { label: string; value: string; caption: string; icon: string; tone: string; accent: string; }
interface InvoiceRow { id: string; patient: string; initials: string; date: string; amount: string; status: string; badge: string; }
interface Alerte { type: 'success' | 'info' | 'warning' | 'danger'; icone: string; titre: string; texte: string; }

const FACTURES: InvoiceRow[] = [
  { id: 'FAC-142', patient: 'Lalle Mohamed', initials: 'LM', date: '15/09/2026', amount: '12 500 MRU', status: 'Payée', badge: 'payee' },
  { id: 'FAC-141', patient: 'Mohamed Sidi', initials: 'MS', date: '15/09/2026', amount: '8 000 MRU', status: 'Partielle', badge: 'partielle' },
  { id: 'FAC-140', patient: 'Fatimetou Ahmedou', initials: 'FA', date: '14/09/2026', amount: '25 000 MRU', status: 'En attente', badge: 'en_attente' },
  { id: 'FAC-139', patient: 'Brahim Ely', initials: 'BE', date: '13/09/2026', amount: '6 500 MRU', status: 'Annulée', badge: 'annulee' }
];
const ALERTES: Alerte[] = [
  { type: 'success', icone: '✓', titre: 'Paiement enregistré', texte: 'La facture FAC-142 est désormais soldée.' },
  { type: 'info', icone: 'i', titre: 'Information', texte: 'Le planning du 18 septembre est encore vide.' },
  { type: 'warning', icone: '!', titre: 'Attention', texte: 'Trois factures dépassent trente jours d\'ancienneté.' },
  { type: 'danger', icone: '×', titre: 'Erreur', texte: 'Le service de facturation est momentanément indisponible.' }
];
const FORMULAIRE = { patient: 'Lalle Mohamed', medecin: 'Dr. Mohamed Cheikh', date: '2026-09-15', statut: 'Confirmé', motif: 'Consultation de contrôle' };

/**
 * Design system vivant : chaque composant de la charte est manipulable (menu, boutons, tableau, formulaire,
 * alertes, progression) et les jetons de couleur affichent leur valeur reelle, mise a jour quand
 * l'administrateur personnalise l'interface.
 */
@Component({
  selector: 'app-ui-kit',
  imports: [FormsModule, PersonnalisationComponent],
  standalone: true,
  templateUrl: './ui-kit.component.html',
  styleUrl: './ui-kit.component.css'
})
export class UiKitComponent {
  readonly swatches: Swatch[] = [
    { label: 'Fond application', token: '--bg-app' },
    { label: 'Surface', token: '--bg-surface' },
    { label: 'Fond discret', token: '--bg-subtle' },
    { label: 'Bordure', token: '--border' },
    { label: 'Texte principal', token: '--text-strong' },
    { label: 'Texte secondaire', token: '--text-muted' },
    { label: 'Marque', token: '--brand' },
    { label: 'Marque sombre', token: '--brand-dark' },
    { label: 'Succès', token: '--accent' },
    { label: 'Alerte', token: '--warn' },
    { label: 'Danger', token: '--danger' },
    { label: 'Information', token: '--info' },
    { label: 'Violet', token: '--violet' },
    { label: 'Bouton principal', token: '--ink' }
  ];

  readonly kpis: Kpi[] = [
    { label: 'Rendez-vous aujourd\'hui', value: '12', caption: '+2 par rapport à hier', icon: '□', tone: 'teal', accent: 'accent' },
    { label: 'Patients enregistrés', value: '350', caption: '+5 ce mois', icon: '○', tone: 'blue', accent: 'brand' },
    { label: 'Factures en attente', value: '04', caption: 'À relancer cette semaine', icon: '▣', tone: 'orange', accent: 'warn' },
    { label: 'Chiffre d\'affaires', value: '125 000', caption: 'MRU encaissés aujourd\'hui', icon: '₨', tone: 'violet', accent: 'violet' }
  ];

  readonly badges: string[][] = [
    ['planifie', 'Planifié'], ['confirme', 'Confirmé'], ['en_cours', 'En cours'], ['termine', 'Terminé'], ['annule', 'Annulé'],
    ['en_attente', 'En attente'], ['partielle', 'Partielle'], ['payee', 'Payée'], ['annulee', 'Annulée']
  ];

  readonly navPreview: { libelle: string; badge?: string; desactive?: boolean }[] = [
    { libelle: 'Tableau de bord' }, { libelle: 'Patients' }, { libelle: 'Rendez-vous' }, { libelle: 'Facturation' },
    { libelle: 'Catalogue des actes', badge: 'Bientôt', desactive: true }
  ];

  /** Valeur reelle de chaque jeton (relue quand l'apparence change). */
  readonly valeurs = signal<Record<string, string>>({});
  readonly navActif = signal('Tableau de bord');
  readonly toast = signal<{ texte: string; type: string } | null>(null);
  readonly factures = signal<InvoiceRow[]>([...FACTURES]);
  readonly factureOuverte = signal<InvoiceRow | null>(null);
  readonly actualisation = signal(false);
  readonly alertes = signal<Alerte[]>([...ALERTES]);
  readonly chargement = signal(true);
  readonly badgeChoisi = signal('');
  progressionPayees = 72;
  progressionPlanning = 48;
  formulaire = { ...FORMULAIRE };
  formulaireErreur = '';
  private minuterieToast?: ReturnType<typeof setTimeout>;

  constructor() {
    this.lireValeurs();
    // Les couleurs changent quand l'administrateur personnalise l'interface (styles poses sur <html>).
    const observateur = new MutationObserver(() => this.lireValeurs());
    observateur.observe(document.documentElement, { attributes: true, attributeFilter: ['style', 'data-theme'] });
    inject(DestroyRef).onDestroy(() => { observateur.disconnect(); clearTimeout(this.minuterieToast); });
  }

  // --- Jetons ------------------------------------------------------------------------------------

  lireValeurs(): void {
    const styles = getComputedStyle(document.documentElement);
    this.valeurs.set(Object.fromEntries(this.swatches.map((s) => [s.token, styles.getPropertyValue(s.token).trim()])));
  }

  copierJeton(swatch: Swatch): void {
    const valeur = this.valeurs()[swatch.token];
    this.copier(`${swatch.token}: ${valeur};`, `${swatch.label} copié : ${valeur}`);
  }

  exporterJetons(): void {
    const contenu = JSON.stringify(this.valeurs(), null, 2);
    const lien = document.createElement('a');
    lien.href = URL.createObjectURL(new Blob([contenu], { type: 'application/json' }));
    lien.download = 'jetons-design-system.json';
    lien.click();
    URL.revokeObjectURL(lien.href);
    this.notifier(`${this.swatches.length} jetons exportés (jetons-design-system.json).`, 'success');
  }

  copierCss(): void {
    const css = ':root {\n' + Object.entries(this.valeurs()).map(([jeton, valeur]) => `  ${jeton}: ${valeur};`).join('\n') + '\n}';
    this.copier(css, 'Variables CSS copiées dans le presse-papiers.');
  }

  // --- Navigation, boutons, badges ---------------------------------------------------------------

  choisirNav(item: { libelle: string; desactive?: boolean }): void {
    if (item.desactive) { this.notifier(`« ${item.libelle} » est désactivé (bientôt disponible).`, 'warning'); return; }
    this.navActif.set(item.libelle);
  }

  action(libelle: string, type = 'info'): void { this.notifier(`Action « ${libelle} » déclenchée.`, type); }

  actionDangereuse(libelle: string): void {
    if (confirm(`Confirmer : ${libelle} ?`)) this.notifier(`« ${libelle} » confirmé.`, 'danger');
  }

  choisirBadge(badge: string[]): void {
    this.badgeChoisi.set(badge[0]);
    this.notifier(`Statut « ${badge[1]} » : classe CSS .badge.${badge[0]}`, 'info');
  }

  // --- Tableau -----------------------------------------------------------------------------------

  actualiser(): void {
    this.actualisation.set(true);
    this.factureOuverte.set(null);
    setTimeout(() => {
      this.factures.set([...FACTURES]);
      this.actualisation.set(false);
      this.notifier('Tableau actualisé.', 'success');
    }, 700);
  }

  voir(facture: InvoiceRow): void { this.factureOuverte.set(this.factureOuverte()?.id === facture.id ? null : facture); }

  supprimer(facture: InvoiceRow): void {
    if (!confirm(`Supprimer ${facture.id} de l’exemple ?`)) return;
    this.factures.update((liste) => liste.filter((ligne) => ligne.id !== facture.id));
    if (this.factureOuverte()?.id === facture.id) this.factureOuverte.set(null);
    this.notifier(`${facture.id} retirée de l’exemple.`, 'warning');
  }

  // --- Formulaire, alertes, etats ----------------------------------------------------------------

  enregistrerFormulaire(): void {
    if (!this.formulaire.patient.trim() || !this.formulaire.date || !this.formulaire.motif.trim()) {
      this.formulaireErreur = 'Le patient, la date et le motif sont obligatoires.';
      return;
    }
    this.formulaireErreur = '';
    this.notifier(`Rendez-vous de ${this.formulaire.patient} (${this.formulaire.statut.toLowerCase()}) enregistré dans l’exemple.`, 'success');
  }

  annulerFormulaire(): void {
    this.formulaire = { ...FORMULAIRE };
    this.formulaireErreur = '';
    this.notifier('Formulaire réinitialisé.', 'info');
  }

  fermerAlerte(alerte: Alerte): void { this.alertes.update((liste) => liste.filter((item) => item !== alerte)); }
  restaurerAlertes(): void { this.alertes.set([...ALERTES]); }

  private copier(texte: string, message: string): void {
    navigator.clipboard?.writeText(texte).then(
      () => this.notifier(message, 'success'),
      () => this.notifier('Copie impossible : le navigateur refuse l’accès au presse-papiers.', 'danger')
    ) ?? this.notifier('Copie impossible dans ce navigateur.', 'danger');
  }

  private notifier(texte: string, type: string): void {
    clearTimeout(this.minuterieToast);
    this.toast.set({ texte, type });
    this.minuterieToast = setTimeout(() => this.toast.set(null), 3500);
  }
}
