import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsultationDossier, DossierPatient } from '../../core/models/consultation';
import { ConsultationService } from '../../core/consultations/consultation.service';
import { AuthService } from '../../core/auth/auth.service';
import { PatientService } from '../../core/patients/patient.service';
import { exporterDossier, lireFichierDossier } from '../../core/patients/dossier-fichier';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { telecharger } from '../../core/http/telechargement';
import { libelleResultat, libelleStatutActe, libelleTypeActe } from '../../core/models/acte';

type Onglet = 'consultations' | 'ordonnances' | 'actes' | 'rendez-vous' | 'factures';

@Component({
  selector: 'app-dossier-patient',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './dossier-patient.component.html',
  styleUrl: './dossier-patient.component.css'
})
export class DossierPatientComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(ConsultationService);
  private readonly patientService = inject(PatientService);
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  readonly libelleTypeActe = libelleTypeActe;
  readonly libelleStatutActe = libelleStatutActe;
  readonly libelleResultat = libelleResultat;
  readonly role = this.auth.role();
  readonly isDirection = this.role === 'DIRECTION';
  readonly isMedecin = this.role === 'MEDECIN';
  readonly isAccueil = this.role === 'ACCUEIL';

  patientId = Number(this.route.snapshot.paramMap.get('patientId'));
  dossier: DossierPatient | null = null;
  onglet: Onglet = 'consultations';
  loading = true;
  importEnCours = false;
  error = '';
  message = '';
  erreurAction = '';

  constructor() {
    this.route.paramMap.subscribe((params) => {
      this.patientId = Number(params.get('patientId'));
      this.charger();
    });
  }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.service.dossier(this.patientId).subscribe({
      next: (dossier) => { this.dossier = dossier; this.loading = false; },
      error: (response) => {
        this.error = response.status === 403
          ? 'Ce dossier médical ne fait pas partie de vos patients.'
          : response.status === 404 ? 'Ce patient n’existe pas.' : 'Le dossier médical est momentanément indisponible.';
        this.loading = false;
      }
    });
  }

  /** Export du dossier complet en PDF, genere par le serveur (JasperReports). */
  exporterPdf(): void {
    if (!this.dossier) return;
    const patient = this.dossier.patient;
    this.erreurAction = '';
    telecharger(this.http, `${environment.apiUrl}/patients/${patient.id}/dossier.pdf`, `dossier-${patient.nom}-${patient.prenom}.pdf`.toLowerCase())
      .subscribe({
        next: () => this.message = 'Dossier exporté en PDF.',
        error: () => this.erreurAction = 'L’export PDF du dossier a échoué.'
      });
  }

  /** Export de transfert (JSON) : seul format reimportable dans l'application. */
  exporter(): void {
    if (!this.dossier) return;
    exporterDossier(this.dossier, this.auth.emailAffiche());
    this.message = 'Dossier exporté : le fichier JSON peut être réimporté dans l’application.';
  }

  async importer(evenement: Event): Promise<void> {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    champ.value = '';
    if (!fichier) return;
    this.message = '';
    this.erreurAction = '';
    try {
      const contenu = await lireFichierDossier(fichier);
      this.importEnCours = true;
      this.patientService.importerDossier(contenu).subscribe({
        next: (bilan) => {
          this.importEnCours = false;
          this.message = `${bilan.patientCree ? 'Nouveau patient créé' : 'Dossier fusionné'} : ${bilan.consultationsImportees} consultation(s), ${bilan.ordonnancesImportees} ordonnance(s), ${bilan.rendezVousImportes} rendez-vous et ${bilan.facturesImportees} facture(s) importés${bilan.elementsIgnores ? `, ${bilan.elementsIgnores} élément(s) ignoré(s)` : ''}.`;
          if (bilan.patientId === this.patientId) this.charger(); else this.router.navigate(['/dossier', bilan.patientId]);
        },
        error: () => { this.importEnCours = false; this.erreurAction = 'L’import du dossier a échoué. Vérifiez le contenu du fichier.'; }
      });
    } catch (erreur) {
      this.erreurAction = (erreur as Error).message;
    }
  }

  get age(): number | null {
    const naissance = this.dossier?.patient.dateNaissance;
    if (!naissance) return null;
    return Math.floor((Date.now() - new Date(naissance).getTime()) / 31557600000);
  }

  get consultationsAvecOrdonnance(): ConsultationDossier[] {
    return this.dossier?.consultations.filter((consultation) => consultation.prescription !== null) ?? [];
  }
  get totalFacture(): number { return this.dossier?.factures.reduce((total, facture) => total + Number(facture.montantTotal), 0) ?? 0; }
  get totalRestant(): number { return this.dossier?.factures.reduce((total, facture) => total + Number(facture.resteAPayer), 0) ?? 0; }

  format(valeur: number): string { return new Intl.NumberFormat('fr-FR').format(valeur) + ' MRU'; }
  statutLabel(statut: string): string {
    return statut.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }
  statutFacture(statut: string): string { return statut === 'PAYEE' ? 'Payée' : statut === 'PARTIELLE' ? 'Partielle' : statut === 'ANNULEE' ? 'Annulée' : 'En attente'; }
}
